package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.dto.ProfileForm;
import com.clothingshop.dto.ProfileView;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.ProfileService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Own profile of any signed-in account (FR-04). {@code SignedInFilter} lets only signed-in users in. */
@WebServlet("/account/profile")
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/account/profile.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("profile", service().get(WebUtil.currentUser(request).getId()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthUser user = WebUtil.currentUser(request);
        ProfileForm form = new ProfileForm(request.getParameter("fullName"), request.getParameter("gender"),
                request.getParameter("dob"), request.getParameter("phone"), WebUtil.parseLong(request.getParameter("version")));
        try {
            ProfileView saved = service().update(user.getId(), form);
            // The header shows the full name kept in the session
            request.getSession().setAttribute(AuthUser.SESSION_KEY,
                    new AuthUser(user.getId(), user.getUserName(), saved.fullName(), user.getRole()));
            FlashMessage.put(request.getSession(), "success", "Đã lưu thông tin cá nhân.");
        } catch (ValidationException e) {
            request.setAttribute("profile", service().get(user.getId()));
            request.setAttribute("form", form);
            request.setAttribute("errors", e.getErrors());
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "warning", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/account/profile");
    }

    private ProfileService service() {
        return AppContextListener.profileService(getServletContext());
    }
}
