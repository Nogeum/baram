(() => {
  const form = document.getElementById('overtimeForm');
  if (!form) return;
  const start = document.getElementById('overtimeStart');
  const end = document.getElementById('overtimeEnd');
  const output = document.getElementById('overtimeDuration');
  const date = form.elements.workDate;
  function update() {
    end.setCustomValidity('');
    const now = new Date(Date.now() + 9 * 60 * 60 * 1000).toISOString();
    date.max = now.slice(0, 10);
    if (!start.value || !end.value) { output.textContent = '시작·종료 시각을 선택하세요.'; return; }
    const minutes = value => { const [h, m] = value.split(':').map(Number); return h * 60 + m; };
    const duration = minutes(end.value) - minutes(start.value);
    if (duration <= 0) { end.setCustomValidity('종료 시각을 시작 시각 이후로 선택하세요.'); output.textContent = '시작·종료 시각을 확인해주세요.'; return; }
    if (date.value && date.value + 'T' + end.value > now.slice(0, 16)) {
      end.setCustomValidity('이미 종료된 초과근무만 신청할 수 있습니다.');
      output.textContent = '미래 날짜나 아직 지나지 않은 시간은 신청할 수 없습니다.';
      return;
    }
    output.textContent = `신청 시간: ${Math.floor(duration / 60)}시간 ${duration % 60}분`;
  }
  start.addEventListener('input', update); end.addEventListener('input', update);
  date.addEventListener('input', update);
  form.addEventListener('submit', event => { update(); if (!form.reportValidity()) event.preventDefault(); });
})();
