package com.clothingshop.controller;

import com.clothingshop.dto.EmployeeForm;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.EmployeeService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Employee accounts, manager only: list, create STAFF accounts (FR-40), lock and unlock (FR-41). */
@WebServlet({"/admin/employees", "/admin/employees/create", "/admin/employees/lock", "/admin/employees/unlock"})
public class EmployeeAdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/staff/employees.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EmployeeService employees = service();
        try {
            switch (request.getServletPath()) {
                case "/admin/employees/create" -> {
                    employees.create(new EmployeeForm(request.getParameter("userName"), request.getParameter("email"),
                            request.getParameter("fullName"), request.getParameter("gender"), request.getParameter("dob"),
                            request.getParameter("phone"), request.getParameter("password"), request.getParameter("confirmPassword")));
                    FlashMessage.put(request.getSession(), "success", "Đã tạo tài khoản nhân viên.");
                }
                case "/admin/employees/lock", "/admin/employees/unlock" -> {
                    boolean unlock = request.getServletPath().endsWith("unlock");
                    Integer id = WebUtil.parseInt(request.getParameter("id"));
                    employees.setActive(id == null ? -1 : id, unlock);
                    FlashMessage.put(request.getSession(), "success", unlock ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            show(request, response);
            return;
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/employees");
    }

    private void show(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("employees", service().list());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private EmployeeService service() {
        return AppContextListener.service(getServletContext(), EmployeeService.class);
    }
}
