(() => {
  const center = document.querySelector('.notification-center');
  if (!center) return;
  const toggle = center.querySelector('#notification-toggle');
  const popup = center.querySelector('#notification-popup');
  const badge = center.querySelector('.notification-count');
  const unread = center.querySelector('#notification-unread');
  const error = center.querySelector('.notification-error');
  const updateLabel = () => toggle.setAttribute('aria-label', Number(unread.textContent) ? `알림, 읽지 않은 알림 ${unread.textContent}개` : '알림');
  const close = (restoreFocus = false) => {
    popup.hidden = true;
    toggle.setAttribute('aria-expanded', 'false');
    if (restoreFocus) toggle.focus();
  };
  toggle.addEventListener('click', () => {
    popup.hidden = !popup.hidden;
    toggle.setAttribute('aria-expanded', String(!popup.hidden));
  });
  center.querySelector('.notification-close').addEventListener('click', () => close(true));
  document.addEventListener('click', event => { if (!center.contains(event.target)) close(); });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !popup.hidden) { close(true); event.preventDefault(); }
  });
  center.addEventListener('focusout', event => {
    if (event.relatedTarget && !center.contains(event.relatedTarget)) close();
  });
  const markRead = async form => {
    const button = form.querySelector('button');
    button.disabled = true;
    error.hidden = true;
    try {
      const response = await fetch(form.action, {
        method: 'POST', body: new URLSearchParams(new FormData(form)),
        credentials: 'same-origin', redirect: 'error'
      });
      if (response.status !== 204) throw new Error('Read failed');
      const item = form.closest('.notification-item');
      item.classList.remove('unread');
      item.querySelector('.notification-read').hidden = false;
      const count = Math.max(0, Number(unread.textContent) - 1);
      unread.textContent = String(count);
      badge.textContent = String(count);
      badge.hidden = count === 0;
      updateLabel();
      form.remove();
      return true;
    } catch {
      error.textContent = '알림을 처리하지 못했습니다. 다시 시도하거나 새로고침 후 로그인 상태를 확인해주세요.';
      error.hidden = false;
      button.disabled = false;
      return false;
    }
  };
  popup.addEventListener('submit', async event => {
    event.preventDefault();
    if (await markRead(event.target)) center.querySelector('.notification-close').focus();
  });
  popup.addEventListener('click', async event => {
    const link = event.target.closest('.notification-link');
    if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const form = link.closest('.notification-item').querySelector('form');
    if (!form) return;
    event.preventDefault();
    if (form.querySelector('button').disabled) return;
    if (await markRead(form)) window.location.assign(link.href);
  });
  updateLabel();
})();
