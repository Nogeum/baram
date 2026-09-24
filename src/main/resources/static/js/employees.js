(() => {
  const create = document.getElementById('employeeCreateDrawer');
  if (!create) return;
  const edit = document.getElementById('employeeEditDrawer');
  const tabs = [...create.querySelectorAll('[data-employee-tab]')];
  function selectTab(id) {
    tabs.forEach(tab => {
      const selected = tab.dataset.employeeTab === id;
      tab.setAttribute('aria-selected', String(selected)); tab.tabIndex = selected ? 0 : -1;
      document.getElementById(tab.dataset.employeeTab).hidden = !selected;
    });
  }
  document.getElementById('openEmployeeCreate').addEventListener('click', () => { selectTab('employeeBasicPanel'); create.showModal(); create.querySelector('[name="name"]').focus(); });
  document.querySelectorAll('[data-close-employee]').forEach(button => button.addEventListener('click', () => button.closest('dialog').close()));
  [create, edit].forEach(dialog => dialog.addEventListener('click', event => {
    const rect = dialog.getBoundingClientRect();
    if (event.target === dialog && (event.clientX < rect.left || event.clientX > rect.right)) dialog.close();
  }));
  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => selectTab(tab.dataset.employeeTab));
    tab.addEventListener('keydown', event => {
      if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) return;
      event.preventDefault(); const next = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
      selectTab(tabs[next].dataset.employeeTab); tabs[next].focus();
    });
  });
  create.querySelector('[data-go-permissions]').addEventListener('click', () => { selectTab('employeePermissionsPanel'); tabs[1].focus(); });
  create.addEventListener('invalid', event => { const panel = event.target.closest('[role="tabpanel"]'); if (panel) selectTab(panel.id); }, true);
  const role = document.getElementById('employeeCreateRole'), manager = document.getElementById('employeeCreateManager');
  role.addEventListener('change', () => { manager.disabled = role.value === 'ADMIN'; if (manager.disabled) manager.value = 'false'; });
  document.querySelectorAll('[data-edit-employee]').forEach(button => button.addEventListener('click', () => {
    const d = button.dataset, form = document.getElementById('employeeEditForm');
    form.action = d.editEmployee;
    const photo=button.closest('[data-profile-id]').dataset;
    const photoForm=document.getElementById('employeePhotoForm'),photoDelete=document.getElementById('employeePhotoDeleteForm');
    photoForm.action='/profiles/'+photo.profileId+'/image';photoForm.reset();
    photoDelete.action=photoForm.action+'/delete';photoDelete.hidden=photo.hasProfile!=='true';
    Object.entries({name:d.name,department:d.department,position:d.position,active:d.active,annualDays:d.annual,departmentManager:d.manager}).forEach(([key, value]) => { form.elements.namedItem(key).value = value; });
    document.getElementById('employeeEditIdentity').textContent = `${d.name} · ${d.login} · ${d.role === 'ADMIN' ? '관리자' : '직원'}`;
    document.getElementById('employeeEditManagerLabel').hidden = d.role === 'ADMIN';
    form.elements.namedItem('departmentManager').disabled = d.role === 'ADMIN';
    const remove = document.getElementById('employeeEditDelete');
    remove.dataset.deleteAccount = d.editEmployee + '/delete'; remove.dataset.login = d.login; remove.dataset.name = d.name; remove.disabled = d.self === 'true';
    document.getElementById('employeeDeleteSelf').hidden = d.self !== 'true';
    edit.showModal(); form.elements.namedItem('name').focus();
  }));
  document.querySelectorAll('.employee-filters select').forEach(select => select.addEventListener('change', () => select.form.requestSubmit()));
  const rows = [...document.querySelectorAll('[data-employee-row]')], size = 10;
  let current = 0;
  const pages = Math.ceil(rows.length / size), previous = document.getElementById('employeePrevious'), next = document.getElementById('employeeNext');
  function renderPage() {
    rows.forEach((row, index) => { row.hidden = Math.floor(index / size) !== current; });
    document.getElementById('employeePageNumber').textContent = String(current + 1);
    document.getElementById('employeePageSummary').textContent = `${current * size + 1}–${Math.min((current + 1) * size, rows.length)} / ${rows.length}명`;
    previous.disabled = current === 0; next.disabled = current >= pages - 1;
  }
  previous.addEventListener('click', () => { if (current > 0) { current--; renderPage(); } });
  next.addEventListener('click', () => { if (current < pages - 1) { current++; renderPage(); } });
  if (rows.length) { document.getElementById('employeePagination').hidden = false; renderPage(); }
})();
