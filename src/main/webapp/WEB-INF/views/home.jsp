<c:set var="pageTitle" value="Trang chủ"/>
<c:set var="activeNav" value="home"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="site-blocks-cover" style="background-image: url('${ctx}/assets/images/hero_1.jpg');" data-aos="fade">
  <div class="container">
    <div class="row align-items-start align-items-md-center justify-content-end">
      <div class="col-md-5 text-center text-md-left pt-5 pt-md-0">
        <h1 class="mb-2">Mặc đẹp mỗi ngày, giá vừa túi tiền</h1>
        <div class="intro-text text-center text-md-left">
          <p class="mb-4">Áo, quần, váy, giày đủ màu và kích thước. Đặt hàng trong vài bước, thanh toán khi nhận hàng hoặc qua VNPAY.</p>
          <p>
            <c:choose>
              <c:when test="${empty sessionScope.auth}">
                <a href="${ctx}/register" class="btn btn-sm btn-primary">Đăng ký tài khoản</a>
              </c:when>
              <c:otherwise>
                <a href="${ctx}/shop" class="btn btn-sm btn-primary">Mua sắm ngay</a>
              </c:otherwise>
            </c:choose>
          </p>
        </div>
      </div>
    </div>
  </div>
</div>

<div class="site-section site-section-sm site-blocks-1">
  <div class="container">
    <div class="row">
      <div class="col-md-6 col-lg-4 d-lg-flex mb-4 mb-lg-0 pl-4" data-aos="fade-up">
        <div class="icon mr-4 align-self-start"><span class="icon-truck"></span></div>
        <div class="text">
          <h2 class="text-uppercase">Phí giao hàng 20.000 ₫</h2>
          <p>Một mức phí cho mọi đơn hàng. Giao lại hàng do sự cố vận chuyển không tính thêm phí.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4 d-lg-flex mb-4 mb-lg-0 pl-4" data-aos="fade-up" data-aos-delay="100">
        <div class="icon mr-4 align-self-start"><span class="icon-refresh2"></span></div>
        <div class="text">
          <h2 class="text-uppercase">Trả hàng trong 15 ngày</h2>
          <p>Trả lại toàn bộ đơn trong 15 ngày kể từ khi nhận hàng; cửa hàng hoàn tiền sau khi duyệt yêu cầu.</p>
        </div>
      </div>
      <div class="col-md-6 col-lg-4 d-lg-flex mb-4 mb-lg-0 pl-4" data-aos="fade-up" data-aos-delay="200">
        <div class="icon mr-4 align-self-start"><span class="icon-help"></span></div>
        <div class="text">
          <h2 class="text-uppercase">Thành viên được ưu đãi</h2>
          <p>Mua càng nhiều, hạng thành viên càng cao, nhận voucher hằng tháng và voucher các dịp lễ.</p>
        </div>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
