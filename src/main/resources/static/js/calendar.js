(() => {
  const modal = document.getElementById('scheduleModal');
  if (!modal) return;
  const form = document.getElementById('scheduleForm');
  const date = document.getElementById('scheduleDate');
  const start = document.getElementById('scheduleStart');
  const end = document.getElementById('scheduleEnd');
  const detail = document.getElementById('scheduleDetail');

  const createAction = form.action;
  let selectedEvent;
  function openRegistration(value) {
    form.reset();
    form.action = createAction;
    document.getElementById('scheduleModalTitle').textContent = '개인 일정 등록';
    end.setCustomValidity('');
    date.value = value;
    modal.showModal();
  }
  document.getElementById('addSchedule').addEventListener('click', event => openRegistration(event.currentTarget.dataset.date));
  document.querySelectorAll('[data-add-date]').forEach(button => {
    button.addEventListener('click', () => openRegistration(button.dataset.addDate));
  });
  document.querySelectorAll('[data-close-dialog]').forEach(button => {
    button.addEventListener('click', () => button.closest('dialog').close());
  });
  [modal, detail].forEach(dialog => {
    dialog.addEventListener('click', event => {
      if (event.target !== dialog) return;
      const box = dialog.getBoundingClientRect();
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) dialog.close();
    });
  });
  function validateTimes() {
    const valid = (!start.value && !end.value) || (start.value && end.value && end.value > start.value);
    end.setCustomValidity(valid ? '' : '시작·종료 시간을 모두 입력하고 종료 시간을 시작 시간 이후로 선택하세요.');
  }
  start.addEventListener('input', validateTimes);
  end.addEventListener('input', validateTimes);
  form.addEventListener('submit', event => {
    validateTimes();
    if (!form.reportValidity()) event.preventDefault();
  });
  document.querySelectorAll('.calendar-event').forEach(button => {
    button.addEventListener('click', () => {
      const data = button.dataset;
      selectedEvent = data;
      document.getElementById('editSchedule').hidden = data.assigned === 'true';
      document.getElementById('detailTitle').textContent = data.title;
      document.getElementById('detailDate').textContent = data.date;
      document.getElementById('detailTime').textContent = data.time;
      document.getElementById('detailKind').textContent = data.assigned === 'true' ? '부서 배정' : '개인 일정';
      document.getElementById('detailColor').style.backgroundColor = data.colorHex;
      document.getElementById('detailColor').setAttribute('aria-label', data.colorLabel);
      document.getElementById('detailMemo').textContent = data.memo || '메모가 없습니다.';
      const deletion = document.getElementById('scheduleDeleteForm');
      deletion.hidden = data.assigned === 'true';
      deletion.action = data.deleteUrl;
      detail.showModal();
    });
  });
  document.getElementById('editSchedule').addEventListener('click', () => {
    if (!selectedEvent || selectedEvent.assigned === 'true') return;
    detail.close();
    openRegistration(selectedEvent.date);
    form.action = selectedEvent.editUrl;
    document.getElementById('scheduleModalTitle').textContent = '개인 일정 수정';
    document.getElementById('scheduleTitle').value = selectedEvent.title;
    start.value = selectedEvent.start || '';
    end.value = selectedEvent.end || '';
    form.elements.memo.value = selectedEvent.memo || '';
    form.elements.color.value = selectedEvent.color;
  });
  document.getElementById('scheduleDeleteForm').addEventListener('submit', event => {
    if (!window.confirm('이 일정을 삭제할까요?')) event.preventDefault();
  });
})();
