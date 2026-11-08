(function () {
  var bar = document.querySelector('.topbar');
  var toggle = bar.querySelector('.menu-toggle');
  var links = bar.querySelectorAll('.nav-links a');

  function update() {
    bar.classList.toggle('scrolled', bar.hasAttribute('data-solid') || window.scrollY > 24 || bar.classList.contains('open'));
  }
  window.addEventListener('scroll', update, { passive: true });
  update();

  function setOpen(open) {
    bar.classList.toggle('open', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.textContent = open ? 'Close' : 'Menu';
    update();
  }
  toggle.addEventListener('click', function () {
    setOpen(!bar.classList.contains('open'));
  });
  links.forEach(function (link) {
    link.addEventListener('click', function () {
      setOpen(false);
    });
  });
  bar.addEventListener('click', function (event) {
    if (event.target === bar && bar.classList.contains('open')) {
      setOpen(false);
    }
  });
  document.addEventListener('click', function (event) {
    if (bar.classList.contains('open') && !bar.contains(event.target)) {
      setOpen(false);
    }
  });
  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && bar.classList.contains('open')) {
      setOpen(false);
      toggle.focus();
    }
  });

  var sections = Array.prototype.map.call(links, function (link) {
    var href = link.getAttribute('href');
    return href.indexOf('#') === 0 ? document.getElementById(href.slice(1)) : null;
  }).filter(Boolean);
  var observer = new IntersectionObserver(function () {
    var active = null;
    sections.forEach(function (section) {
      if (section.getBoundingClientRect().top < window.innerHeight * 0.4) {
        active = section.id;
      }
    });
    links.forEach(function (link) {
      if (link.getAttribute('href').indexOf('#') === 0) {
        link.setAttribute('aria-current', String(link.getAttribute('href') === '#' + active));
      }
    });
  }, { threshold: [0, 0.25, 0.5, 0.75, 1] });
  sections.forEach(function (section) {
    observer.observe(section);
  });
})();

(function () {
  var viewer = document.querySelector('.viewer');
  if (!viewer) {
    return;
  }
  var large = viewer.querySelector('img');
  var caption = viewer.querySelector('p');
  var frame = viewer.querySelector('.frame');
  var opener = null;
  var current = null;

  function layout() {
    if (!current) {
      return;
    }
    var width = current.naturalWidth;
    var height = current.naturalHeight;
    var portrait = window.innerHeight > window.innerWidth;
    var turn = portrait && window.innerWidth < 700 && width / height > 2.5;
    viewer.classList.toggle('turned', turn);
    caption.textContent = turn ? current.alt + '. Turn your phone sideways to read it.' : current.alt;
    if (!turn) {
      large.style.width = '';
      large.style.height = '';
      frame.style.width = '';
      frame.style.height = '';
      return;
    }
    var scale = Math.min((window.innerHeight - 150) / width, (window.innerWidth - 32) / height);
    large.style.width = Math.round(width * scale) + 'px';
    large.style.height = Math.round(height * scale) + 'px';
    frame.style.width = Math.round(height * scale) + 'px';
    frame.style.height = Math.round(width * scale) + 'px';
  }

  window.addEventListener('resize', function () {
    if (viewer.open) {
      layout();
    }
  });

  document.querySelectorAll('.shot, .screen img').forEach(function (image) {
    var button = document.createElement('button');
    button.type = 'button';
    button.className = 'zoom';
    button.setAttribute('aria-label', 'Show larger: ' + image.alt);
    image.parentNode.insertBefore(button, image);
    button.appendChild(image);
    button.addEventListener('click', function () {
      opener = button;
      current = image;
      large.src = image.currentSrc || image.src;
      large.alt = image.alt;
      layout();
      viewer.showModal();
    });
  });

  function close() {
    viewer.close();
  }

  viewer.querySelector('.close').addEventListener('click', close);
  viewer.addEventListener('click', function (event) {
    if (event.target === viewer || event.target === caption) {
      close();
    }
  });
  viewer.addEventListener('close', function () {
    current = null;
    large.removeAttribute('src');
    if (opener) {
      opener.focus();
    }
  });
})();
