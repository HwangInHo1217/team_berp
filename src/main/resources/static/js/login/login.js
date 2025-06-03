/**
 * login.js
 *  - ID: admin / PW: admin123456 으로 하드코딩 검사
 *  - 성공하면 "/dashboard" 로 이동
 */

document.addEventListener('DOMContentLoaded', () => {
  const loginBtn = document.getElementById('loginBtn');
  const usernameInput = document.getElementById('username');
  const passwordInput = document.getElementById('password');

  loginBtn.addEventListener('click', () => {
    const username = usernameInput.value.trim();
    const password = passwordInput.value.trim();

    if (username === 'admin' && password === 'admin123456') {
      // 로그인 성공 → 대시보드로 이동
      window.location.href = '/index';
    } else {
      alert('아이디 또는 비밀번호가 잘못되었습니다.');
      passwordInput.value = '';
      passwordInput.focus();
    }
  });
});
