// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.account.internal.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.account.User;
import com.google.account.filter.FilterUtil;
import com.google.account.filter.SessionInfo;
import com.google.account.filter.SessionUtil;
import com.google.account.internal.storage.UserStore;
import com.google.account.internal.storage.UserStore.SignupError;
import com.google.common.collect.Lists;
import com.google.config.UrlConfig;
import com.google.dosidos.AppContextListener;
import com.google.dosidos.TwoTokenManager;
import com.google.dosidos.data.SessionState;

public class SignupServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static Random random = new Random();

  private TwoTokenManager getTwoTokenManager() {
    return (TwoTokenManager) getServletContext().getAttribute(
        AppContextListener.ATTR_TWO_TOKEN_MANAGER);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    List<SignupError> errors = Lists.newArrayList();
    @SuppressWarnings("unchecked")
    User user = UserStore.signup(req.getParameterMap(), req.getRemoteAddr(), errors);
    if (user != null) {
      long expiresAt = System.currentTimeMillis() + SessionUtil.SESSION_LIFETIME * 1000L;
      SessionInfo sessionInfo = new SessionInfo(user.getUid(), expiresAt, random.nextLong());
      FilterUtil.setSessionCookie(req, resp, sessionInfo);
      FilterUtil.setSessionInfoInRequestAttributeAfterLogin(req, user, sessionInfo);

      String uid = String.valueOf(user.getUid());
      TwoTokenManager twoTokenManager = getTwoTokenManager();
      if (twoTokenManager.isEnabled(req.getHeader("User-Agent"), uid)) {
        SessionState sessionState = twoTokenManager.generateSessionStateOnLogin(
            uid, req.getServerName(), SessionUtil.SESSION_LIFETIME);
        // This will trigger the JSP page to install service worker
        req.setAttribute("login_session_state", sessionState);
      }
      req.getRequestDispatcher(UrlConfig.HOME_PAGE).forward(req, resp);
    } else {
      req.setAttribute("errors", errors);
      RequestDispatcher dispatcher =
          getServletContext().getRequestDispatcher(UrlConfig.SIGNUP_PAGE);
      dispatcher.forward(req, resp);
    }
  }
}
