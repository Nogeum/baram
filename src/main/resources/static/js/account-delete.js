(() => {
  const dialog = document.getElementById('accountDeleteDialog');
  if (!dialog) return;
  const form = document.getElementById('accountDeleteForm');
  const input = document.getElementById('accountDeleteConfirm');
  const submit = document.getElementById('accountDeleteSubmit');
  let login = '';
  document.querySelectorAll('[data-delete-account]').forEach(button => button.addEventListener('click', () => {
    login = button.dataset.login;
    form.reset();
    form.action = button.dataset.deleteAccount;
    submit.disabled = true;
    document.getElementById('accountDeleteTarget').textContent = `${button.dataset.name} (${login})`;
    dialog.showModal();
    input.focus();
  }));
  input.addEventListener('input', () => { submit.disabled = input.value !== login; });
  form.addEventListener('submit', event => {
    if (!login || input.value !== login) { event.preventDefault(); return; }
    submit.disabled = true;
  });
  document.getElementById('accountDeleteCancel').addEventListener('click', () => dialog.close());
})();
