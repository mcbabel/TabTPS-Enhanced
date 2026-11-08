(function () {
  var toc = document.querySelector('.doc-toc');
  var links = Array.prototype.slice.call(toc.querySelectorAll('.group a'));
  var search = toc.querySelector('input');
  var empty = toc.querySelector('.doc-search-empty');
  var current = toc.querySelector('.doc-current');
  var sections = links.map(function (link) {
    return document.getElementById(link.getAttribute('href').slice(1));
  });

  if (window.matchMedia('(max-width: 900px)').matches) {
    toc.open = false;
  }

  function filter() {
    var query = search.value.trim().toLowerCase();
    var shown = 0;
    links.forEach(function (link, index) {
      var match = !query || sections[index].textContent.toLowerCase().indexOf(query) !== -1;
      link.parentElement.hidden = !match;
      if (match) {
        shown++;
      }
    });
    toc.querySelectorAll('.group').forEach(function (group) {
      group.hidden = !group.querySelector('li:not([hidden])');
    });
    empty.hidden = shown !== 0;
  }
  search.addEventListener('input', filter);
  search.addEventListener('keydown', function (event) {
    if (event.key !== 'Enter') {
      return;
    }
    var first = links.find(function (link) {
      return !link.parentElement.hidden;
    });
    if (first) {
      event.preventDefault();
      first.click();
    }
  });

  links.forEach(function (link) {
    link.addEventListener('click', function () {
      if (window.matchMedia('(max-width: 900px)').matches) {
        toc.open = false;
      }
    });
  });

  function spy() {
    var active = null;
    sections.forEach(function (section) {
      if (section.getBoundingClientRect().top < 140) {
        active = section;
      }
    });
    links.forEach(function (link, index) {
      link.setAttribute('aria-current', String(sections[index] === active));
      if (sections[index] === active) {
        current.textContent = link.textContent;
      }
    });
    if (!active) {
      current.textContent = '';
    }
  }
  window.addEventListener('scroll', spy, { passive: true });
  spy();

  document.querySelectorAll('.doc-content pre').forEach(function (pre) {
    var wrapper = document.createElement('div');
    wrapper.className = 'code-block';
    pre.parentNode.insertBefore(wrapper, pre);
    var head = document.createElement('div');
    head.className = 'code-head';
    wrapper.appendChild(head);
    wrapper.appendChild(pre);
    var button = document.createElement('button');
    button.type = 'button';
    button.className = 'copy-button';
    button.textContent = 'Copy';
    button.addEventListener('click', function () {
      var text = pre.textContent;
      var done = function () {
        button.textContent = 'Copied';
        setTimeout(function () {
          button.textContent = 'Copy';
        }, 1600);
      };
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(done);
      } else {
        var area = document.createElement('textarea');
        area.value = text;
        document.body.appendChild(area);
        area.select();
        document.execCommand('copy');
        area.remove();
        done();
      }
    });
    head.appendChild(button);
  });

  document.querySelectorAll('.doc-content table').forEach(function (table) {
    var labels = Array.prototype.map.call(table.querySelectorAll('thead th'), function (th) {
      return th.textContent;
    });
    table.querySelectorAll('tbody tr').forEach(function (row) {
      Array.prototype.forEach.call(row.children, function (cell, index) {
        if (labels[index]) {
          cell.setAttribute('data-label', labels[index]);
        }
      });
    });
  });
})();
