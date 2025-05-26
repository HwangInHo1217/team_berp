// 현재 URL과 같은 메뉴에 active 클래스 부여
document.addEventListener('DOMContentLoaded', function () {
  const menus = document.querySelectorAll('.sub-menu');
  const path = location.pathname;
  menus.forEach(a => {
    if (a.getAttribute('href') && path.endsWith(a.getAttribute('href').split('/').pop())) {
      a.classList.add('active');
      // 상위 메뉴도 펼침
      const parentCollapse = a.closest('.collapse');
      if (parentCollapse) {
        parentCollapse.classList.add('show');
        const parentToggle = parentCollapse.previousElementSibling;
        if (parentToggle) parentToggle.setAttribute('aria-expanded', 'true');
      }
    }
  });
});
