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
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.account.filter.FilterUtil;
import com.google.account.filter.SessionInfo;
import com.google.account.filter.SessionUtil;
import com.google.account.internal.UserImpl;
import com.google.account.internal.storage.UserStore;
import com.google.config.UrlConfig;

public class LoginServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static Random random = new Random();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String email = req.getParameter("email");
    String password = req.getParameter("password");

    if (UserStore.login(email, password)) {
      UserImpl user = UserStore.getUserByEmail(email);
      long expiresAt = System.currentTimeMillis() + SessionUtil.SESSION_LIFETIME * 1000;
      SessionInfo sessionInfo = new SessionInfo(user.getUid(), expiresAt, random.nextLong());
      FilterUtil.setSessionCookie(req, resp, sessionInfo);
      FilterUtil.setSessionInfoInRequestAttributeAfterLogin(req, user, sessionInfo);
      req.getRequestDispatcher(UrlConfig.HOME_PAGE).forward(req, resp);
    } else {
      req.setAttribute("error", "WRONG_PASSWORD");
      RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(
          UrlConfig.LOGIN_PAGE);
      dispatcher.forward(req, resp);
    }
  }
}
