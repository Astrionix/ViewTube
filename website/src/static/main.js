/* viewtube.org: small progressive enhancements, no dependencies. Every page works without them.
   The release links, sizes and star count are written into the pages at build time (build.py),
   so this file makes no network requests.
   1. The chapter demo: chapters seek the clip and fill as it plays.
   2. Copy buttons & Share hub.
   3. Screen switcher mockup tabs.
   4. Lightbox screenshot preview modal.
   5. Links to the old one-page site's #anchors forward to the pages that replaced them.
   6. A link to a FAQ answer opens it.
   7. The speed numbers run like a stopwatch, in real time. */
(function () {
  "use strict";

  var LANG = (document.documentElement.lang || "en").slice(0, 2);

  /* ---------- 3. Old anchors ---------- */
  (function forwardOldAnchors() {
    var path = location.pathname.replace(/index\.html$/, "");
    if (path !== "/") return;
    var map = {
      "#download": "/download/",
      "#trust": "/trust/",
      "#faq": "/faq/",
      "#sign-in": "/faq/#sign-in",
      "#how-fast": "#speed",
      "#smarttube": "/faq/#smarttube-on-phones",
      "#smarttube-for-phones": "/faq/#smarttube-on-phones",
      "#built-on-smarttube": "/faq/#smarttube-on-phones"
    };
    var to = map[location.hash];
    if (!to) return;
    if (to.charAt(0) === "#") {
      history.replaceState(null, "", to);
      var el = document.querySelector(to);
      if (el) el.scrollIntoView();
    } else {
      location.replace(to);
    }
  })();

  /* ---------- 1. Chapter demo ---------- */
  function setupDemo() {
    var root = document.querySelector("[data-demo]");
    if (!root) return;
    var video = root.querySelector("[data-demo-video]");
    var toggle = root.querySelector("[data-demo-toggle]");
    var buttons = root.querySelectorAll(".chapters button");
    if (!video || !buttons.length) return;

    var words = LANG === "es" ? { play: "Reproducir", pause: "Pausa" } : { play: "Play", pause: "Pause" };
    var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var saveData = navigator.connection && navigator.connection.saveData;
    var autoplay = !reduce && !saveData;
    var userPaused = false;
    var visible = false;

    var chapters = [];
    for (var i = 0; i < buttons.length; i++) {
      chapters.push({
        el: buttons[i],
        fill: buttons[i].querySelector(".ch-bar i"),
        start: parseFloat(buttons[i].getAttribute("data-start")) || 0,
        end: parseFloat(buttons[i].getAttribute("data-end")) || 0
      });
    }

    function setToggle() {
      if (!toggle) return;
      var playing = !video.paused;
      toggle.textContent = playing ? words.pause : words.play;
      toggle.setAttribute("aria-pressed", playing ? "true" : "false");
    }

    function play() {
      if (video.preload === "none") video.preload = "auto";
      var p = video.play();
      if (p && p.catch) p.catch(function () { setToggle(); });
    }

    var raf = 0;
    function paint() {
      var t = video.currentTime;
      for (var c = 0; c < chapters.length; c++) {
        var ch = chapters[c];
        var p = t >= ch.end ? 1 : t <= ch.start ? 0 : (t - ch.start) / (ch.end - ch.start);
        if (ch.fill) ch.fill.style.width = (p * 100).toFixed(2) + "%";
        var on = t >= ch.start && t < ch.end;
        ch.el.parentNode.classList.toggle("on", on);
        if (on) ch.el.setAttribute("aria-current", "true"); else ch.el.removeAttribute("aria-current");
      }
      raf = video.paused ? 0 : requestAnimationFrame(paint);
    }

    video.addEventListener("play", function () { setToggle(); if (!raf) raf = requestAnimationFrame(paint); });
    video.addEventListener("pause", function () { setToggle(); paint(); });
    video.addEventListener("seeked", paint);

    for (var b = 0; b < chapters.length; b++) {
      (function (ch) {
        ch.el.addEventListener("click", function () {
          if (video.preload === "none") video.preload = "auto";
          video.currentTime = ch.start + 0.05;
          userPaused = false;
          play();
        });
      })(chapters[b]);
    }

    if (toggle) {
      toggle.addEventListener("click", function () {
        if (video.paused) { userPaused = false; play(); } else { userPaused = true; video.pause(); }
      });
    }

    if ("IntersectionObserver" in window) {
      new IntersectionObserver(function (entries) {
        visible = entries[0].isIntersecting;
        if (visible && autoplay && !userPaused) play();
        else if (!visible && !video.paused) video.pause();
      }, { threshold: 0.35 }).observe(video);
    }
    setToggle();
    paint();
  }

  /* ---------- 2. Copy buttons ---------- */
  function setupCopy() {
    if (!navigator.clipboard || !window.isSecureContext) return;
    var status = document.querySelector("[data-copy-status]");
    var buttons = document.querySelectorAll("[data-copy]");
    var words = LANG === "es" ? { copy: "Copiar", done: "Copiado", said: "Copiado al portapapeles." }
                              : { copy: "Copy", done: "Copied", said: "Copied to the clipboard." };
    for (var i = 0; i < buttons.length; i++) {
      (function (btn) {
        var target = document.getElementById(btn.getAttribute("data-copy"));
        var label = btn.querySelector("span");
        if (!target || !label) return;
        btn.hidden = false;
        var reset = null;
        btn.addEventListener("click", function () {
          navigator.clipboard.writeText(target.textContent.trim()).then(function () {
            label.textContent = words.done;
            if (status) status.textContent = words.said;
            if (navigator.vibrate) { try { navigator.vibrate(10); } catch (e) {} }
            if (btn.animate) {
              btn.animate([
                { transform: 'scale(0.92)' },
                { transform: 'scale(1.04)' },
                { transform: 'scale(1)' }
              ], { duration: 240, easing: 'cubic-bezier(0.16, 1, 0.3, 1)' });
            }
            clearTimeout(reset);
            reset = setTimeout(function () {
              label.textContent = words.copy;
              if (status) status.textContent = "";
            }, 2000);
          }, function () { /* clipboard refused: nothing to do */ });
        });
      })(buttons[i]);
    }
  }

  /* ---------- 4. A link to a FAQ answer opens it ---------- */
  function openTarget() {
    var id = location.hash.slice(1);
    if (!id) return;
    var el = document.getElementById(id);
    if (el && el.tagName === "DETAILS") el.open = true;
  }

  /* ---------- 5. Stopwatch ---------- */
  function setupStopwatch() {
    var list = document.querySelector(".stats");
    if (!list || !("IntersectionObserver" in window) || !window.requestAnimationFrame) return;
    if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    var sep = LANG === "es" ? "," : ".";
    var stats = [].map.call(list.querySelectorAll(".stat .n"), function (el) {
      return { el: el, box: el.parentNode, end: parseFloat(el.textContent.replace(",", ".")), text: el.textContent };
    }).filter(function (s) { return s.end > 0; });
    if (!stats.length) return;

    function show(s, v) { s.el.textContent = v.toFixed(2).replace(".", sep); }
    function run() {
      var t0 = null;
      stats.forEach(function (s) { show(s, 0); s.box.classList.add("ticking"); });
      requestAnimationFrame(function frame(now) {
        if (t0 === null) t0 = now;
        var elapsed = (now - t0) / 1000, running = false;
        stats.forEach(function (s) {
          if (elapsed < s.end) { show(s, elapsed); running = true; }
          else if (s.box.classList.contains("ticking")) { s.el.textContent = s.text; s.box.classList.remove("ticking"); }
        });
        if (running) requestAnimationFrame(frame);
      });
    }
    var io = new IntersectionObserver(function (entries) {
      if (!entries[0].isIntersecting) return;
      io.disconnect();
      run();
    }, { threshold: 0.6 });
    io.observe(list);
  }


  /* ---------- 7. Toast & Share Hub ---------- */
  var toastTimer = null;
  function showToast(msg) {
    var toast = document.getElementById("site-toast");
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      toast.classList.remove("show");
    }, 2800);
  }

  function setupShareHub() {
    var nativeBtn = document.getElementById("native-share-btn");
    var copyBtn = document.getElementById("copy-share-btn");
    var copyBtnText = document.getElementById("copy-btn-text");
    var copyInputBtn = document.getElementById("copy-input-btn");
    var shareInput = document.getElementById("share-url-input");

    var releaseUrl = (shareInput && shareInput.value) || "https://github.com/Astrionix/ViewTube/releases/latest";
    var copiedMsg = LANG === "es" ? "¡Enlace copiado al portapapeles!" : "Link copied to clipboard!";
    var copiedLabel = LANG === "es" ? "¡Copiado!" : "Copied!";
    var copyLabel = LANG === "es" ? "Copiar enlace" : "Copy Link";

    function copyText(val, onSuccess) {
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(val).then(onSuccess, function () { fallbackCopy(val, onSuccess); });
      } else {
        fallbackCopy(val, onSuccess);
      }
    }

    function fallbackCopy(val, onSuccess) {
      try {
        var t = document.createElement("textarea");
        t.value = val;
        t.style.position = "fixed";
        t.style.opacity = "0";
        document.body.appendChild(t);
        t.select();
        document.execCommand("copy");
        document.body.removeChild(t);
        if (onSuccess) onSuccess();
      } catch (e) {}
    }

    if (nativeBtn) {
      nativeBtn.addEventListener("click", function () {
        if (navigator.share) {
          navigator.share({
            title: "ViewTube for Android",
            text: LANG === "es"
              ? "ViewTube: Cliente de YouTube rápido, privado y sin anuncios con reproducción en segundo plano y diseño cristal líquido."
              : "ViewTube: Fast, private, ad-free YouTube client for Android with background playback and liquid glass UI.",
            url: releaseUrl
          }).catch(function () {});
        } else {
          copyText(releaseUrl, function () {
            showToast(copiedMsg);
            if (navigator.vibrate) { try { navigator.vibrate(10); } catch (e) {} }
          });
        }
      });
    }

    if (copyBtn) {
      var resetTimer = null;
      copyBtn.addEventListener("click", function () {
        copyText(releaseUrl, function () {
          if (copyBtnText) copyBtnText.textContent = copiedLabel;
          showToast(copiedMsg);
          if (navigator.vibrate) { try { navigator.vibrate(10); } catch (e) {} }
          clearTimeout(resetTimer);
          resetTimer = setTimeout(function () {
            if (copyBtnText) copyBtnText.textContent = copyLabel;
          }, 2200);
        });
      });
    }

    if (copyInputBtn && shareInput) {
      var inputResetTimer = null;
      copyInputBtn.addEventListener("click", function () {
        copyText(shareInput.value, function () {
          copyInputBtn.textContent = "✓";
          showToast(copiedMsg);
          if (navigator.vibrate) { try { navigator.vibrate(10); } catch (e) {} }
          clearTimeout(inputResetTimer);
          inputResetTimer = setTimeout(function () {
            copyInputBtn.textContent = LANG === "es" ? "Copiar" : "Copy";
          }, 2000);
        });
      });
    }
  }

  /* ---------- 8. Lightbox Screenshot Zoom ---------- */
  function setupLightbox() {
    var modal = document.getElementById("lightbox-modal");
    if (!modal) return;
    var img = document.getElementById("lightbox-img");
    var caption = document.getElementById("lightbox-caption");
    var closeBtn = modal.querySelector(".lightbox-close");
    var backdrop = modal.querySelector(".lightbox-backdrop");
    var triggers = document.querySelectorAll("[data-lightbox]");

    function openModal(src, alt, cap) {
      if (img) { img.src = src; img.alt = alt || ""; }
      if (caption) caption.textContent = cap || alt || "";
      modal.classList.add("open");
      modal.setAttribute("aria-hidden", "false");
      document.body.style.overflow = "hidden";
    }

    function closeModal() {
      modal.classList.remove("open");
      modal.setAttribute("aria-hidden", "true");
      document.body.style.overflow = "";
    }

    triggers.forEach(function (trigger) {
      trigger.style.cursor = "zoom-in";
      trigger.addEventListener("click", function () {
        var cap = trigger.getAttribute("data-caption") || trigger.alt;
        openModal(trigger.src, trigger.alt, cap);
      });
    });

    if (closeBtn) closeBtn.addEventListener("click", closeModal);
    if (backdrop) backdrop.addEventListener("click", closeModal);
    window.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && modal.classList.contains("open")) {
        closeModal();
      }
    });
  }

  setupDemo();
  setupCopy();
  setupStopwatch();
  setupShareHub();
  setupLightbox();
  openTarget();
  window.addEventListener("hashchange", openTarget);
})();
