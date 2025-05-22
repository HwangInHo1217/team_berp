

//공통 레이아웃 fetch
    fetch('/layouts/header.html')
      .then(res => res.text())
      .then(data => document.getElementById('header').innerHTML = data);
    fetch('/layouts/sidebar.html')
      .then(res => res.text())
      .then(data => document.getElementById('sidebar').innerHTML = data);
    fetch('/layouts/footer.html')
      .then(res => res.text())
      .then(data => document.getElementById('footer').innerHTML = data);
