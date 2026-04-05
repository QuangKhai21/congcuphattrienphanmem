/**
 * Chặn điều hướng khi href trùng URL hiện tại (pathname + query).
 * Tránh màn hình trắng trên một số trình duyệt khi bấm cùng một mục menu nhiều lần liên tiếp.
 * Cải thiện để xử lý tốt hơn các trường hợp bị stuck modal/overlay.
 */
(function () {
    'use strict';

    /**
     * Xóa các overlay của Bootstrap bị stuck trên màn hình.
     * Tránh tình trạng modal-backdrop không bị xóa hết gây màn hình mờ/trắng.
     */
    function removeStuckBootstrapOverlays() {
        var body = document.body;

        // Xóa backdrop
        document.querySelectorAll('.modal-backdrop, .offcanvas-backdrop').forEach(function (el) {
            el.remove();
        });

        // Reset body classes
        body.classList.remove('modal-open', 'offcanvas-open');
        body.style.removeProperty('overflow');
        body.style.removeProperty('padding-right');

        // Đóng modal đang hiển thị
        document.querySelectorAll('.modal.show').forEach(function (m) {
            var modalInstance = bootstrap && bootstrap.Modal && bootstrap.Modal.getInstance(m);
            if (modalInstance) {
                modalInstance.hide();
            } else {
                m.classList.remove('show');
                m.style.removeProperty('display');
                m.setAttribute('aria-hidden', 'true');
                m.removeAttribute('aria-modal');
            }
        });

        // Đóng offcanvas đang hiển thị
        document.querySelectorAll('.offcanvas.show').forEach(function (o) {
            var offcanvasInstance = bootstrap && bootstrap.Offcanvas && bootstrap.Offcanvas.getInstance(o);
            if (offcanvasInstance) {
                offcanvasInstance.hide();
            }
        });
    }

    /**
     * Bình thường hóa path (bỏ trailing slash).
     */
    function normalizePath(p) {
        if (!p) return '/';
        return p.length > 1 && p.endsWith('/') ? p.slice(0, -1) : p;
    }

    /**
     * Kiểm tra xem URL có trùng với URL hiện tại không.
     */
    function isSameUrl(a) {
        try {
            var u = new URL(a.href, window.location.href);
            if (u.origin !== window.location.origin) return false;
            return normalizePath(u.pathname) === normalizePath(window.location.pathname) &&
                   u.search === window.location.search;
        } catch (e) {
            return false;
        }
    }

    // Chạy khi DOM sẵn sàng
    document.addEventListener('DOMContentLoaded', function () {
        removeStuckBootstrapOverlays();
    });

    // Chạy khi trang được khôi phục từ bộ nhớ cache (bfcache)
    window.addEventListener('pageshow', function (e) {
        if (e.persisted) {
            removeStuckBootstrapOverlays();
        }
    });

    // Chạy khi trang được activate (back-forward cache)
    window.addEventListener('activate', function () {
        removeStuckBootstrapOverlays();
    });

    // Chặn click vào link trùng URL - cuộn lên đầu trang thay vì reload
    document.addEventListener('click', function (e) {
        var a = e.target.closest && e.target.closest('a[href]');
        if (!a) return;

        // Bỏ qua nếu sự kiện đã bị preventDefault
        if (e.defaultPrevented) return;

        // Chỉ xử lý click trái chuột
        if (e.button !== 0) return;

        // Bỏ qua nếu có modifier keys
        if (e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;

        // Bỏ qua link download
        if (a.getAttribute('download') != null) return;

        // Bỏ qua link mở tab mới
        var t = a.getAttribute('target');
        if (t && t !== '' && t.toLowerCase() !== '_self') return;

        // Bỏ qua link có hash hoặc rỗng
        var hrefAttr = a.getAttribute('href');
        if (!hrefAttr || hrefAttr.startsWith('#') || hrefAttr.startsWith('javascript:')) return;

        // Kiểm tra nếu URL trùng với trang hiện tại
        if (isSameUrl(a)) {
            e.preventDefault();
            // Cuộn lên đầu trang một cách mượt mà
            window.scrollTo({ top: 0, behavior: 'smooth' });

            // Nếu là nav-link active, cập nhật trạng thái
            if (a.classList.contains('nav-link') || a.closest('.nav')) {
                // Trigger re-render nhẹ bằng cách focus và blur
                a.focus();
                setTimeout(function () {
                    a.blur();
                }, 10);
            }
        }
    }, true);

    // Backup: chỉ dọn khi backdrop "mồ côi" (còn màn mờ nhưng không còn modal .show).
    // Trước đây: mọi modal hợp lệ cũng có backdrop → interval đóng modal ngay sau khi mở.
    setInterval(function () {
        var backdrops = document.querySelectorAll('.modal-backdrop');
        var openModals = document.querySelectorAll('.modal.show');
        if (backdrops.length > 0 && openModals.length === 0) {
            removeStuckBootstrapOverlays();
        }
    }, 2000);

})();
