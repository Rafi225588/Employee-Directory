const API_URL = '/api/employees';
const SERVER_DOWN_MESSAGE = 'Could not reach the server. Make sure the backend is running on port 8080.';
const fieldNames = ['fullName', 'email', 'phone', 'department', 'designation', 'joiningDate'];
const allowedFileTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/gif', 'image/webp'];
const maxFileSize = 5 * 1024 * 1024;

const listView = document.getElementById('list-view');
const formView = document.getElementById('form-view');
const listMessage = document.getElementById('list-message');
const successMessage = document.getElementById('success-message');
const formMessage = document.getElementById('form-message');
const tableWrap = document.getElementById('table-wrap');
const tableBody = document.getElementById('employee-table-body');
const emptyMessage = document.getElementById('empty-message');
const formTitle = document.getElementById('form-title');
const employeeForm = document.getElementById('employee-form');
const fileInput = document.getElementById('file');
const currentFile = document.getElementById('current-file');
const fileError = document.getElementById('error-file');
const confirmModal = document.getElementById('confirm-modal');
const confirmTitle = document.getElementById('confirm-title');
const confirmMessage = document.getElementById('confirm-message');
const confirmOkButton = document.getElementById('confirm-ok-button');
const confirmCancelButton = document.getElementById('confirm-cancel-button');

let editingId = null;
let pendingAction = null;
let successTimer = null;

function showMessage(element, text) {
    element.textContent = text;
    element.hidden = text === '';
}

function showSuccess(text) {
    showMessage(successMessage, text);
    clearTimeout(successTimer);
    successTimer = setTimeout(() => showMessage(successMessage, ''), 3000);
}

async function readError(response) {
    try {
        return await response.json();
    } catch {
        return { message: 'Something went wrong. Please try again.' };
    }
}

function openConfirmModal(title, message, okLabel, okClass, action) {
    confirmTitle.textContent = title;
    confirmMessage.textContent = message;
    confirmOkButton.textContent = okLabel;
    confirmOkButton.className = 'btn ' + okClass;
    pendingAction = action;
    confirmModal.hidden = false;
    confirmOkButton.focus();
}

function closeConfirmModal() {
    confirmModal.hidden = true;
    pendingAction = null;
}

function runPendingAction() {
    const action = pendingAction;
    closeConfirmModal();
    if (action) {
        action();
    }
}

function uploadUrl(id) {
    return API_URL + '/' + id + '/file';
}

function createFileLinks(employee) {
    const wrapper = document.createElement('span');
    wrapper.className = 'file-links';

    const viewLink = document.createElement('a');
    viewLink.href = employee.fileUrl;
    viewLink.target = '_blank';
    viewLink.rel = 'noopener';
    viewLink.textContent = 'View';

    const downloadLink = document.createElement('a');
    downloadLink.href = employee.fileUrl;
    downloadLink.download = '';
    downloadLink.textContent = 'Download';

    wrapper.appendChild(viewLink);
    wrapper.appendChild(downloadLink);
    return wrapper;
}

function createCell(text) {
    const cell = document.createElement('td');
    cell.textContent = text;
    return cell;
}

function createButton(label, className, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    button.textContent = label;
    button.addEventListener('click', onClick);
    return button;
}

function renderEmployees(employees) {
    tableBody.innerHTML = '';
    tableWrap.hidden = employees.length === 0;
    emptyMessage.hidden = employees.length !== 0;

    employees.forEach((employee) => {
        const row = document.createElement('tr');
        row.appendChild(createCell(employee.id));
        row.appendChild(createCell(employee.fullName));
        row.appendChild(createCell(employee.email));
        row.appendChild(createCell(employee.phone));
        row.appendChild(createCell(employee.department));
        row.appendChild(createCell(employee.designation || '-'));
        row.appendChild(createCell(employee.joiningDate));

        const fileCell = document.createElement('td');
        if (employee.fileUrl) {
            fileCell.appendChild(createFileLinks(employee));
        } else {
            fileCell.textContent = '-';
        }
        row.appendChild(fileCell);

        const actions = document.createElement('div');
        actions.className = 'actions';
        actions.appendChild(createButton('Edit', 'btn', () => editEmployee(employee.id)));
        actions.appendChild(createButton('Delete', 'btn btn-danger', () => confirmDelete(employee)));

        const actionsCell = document.createElement('td');
        actionsCell.appendChild(actions);
        row.appendChild(actionsCell);

        tableBody.appendChild(row);
    });
}

async function loadEmployees() {
    try {
        const response = await fetch(API_URL);
        if (!response.ok) {
            const error = await readError(response);
            showMessage(listMessage, error.message);
            return;
        }
        renderEmployees(await response.json());
        showMessage(listMessage, '');
    } catch {
        showMessage(listMessage, SERVER_DOWN_MESSAGE);
    }
}

function clearErrors() {
    showMessage(formMessage, '');
    fileError.textContent = '';
    fieldNames.forEach((name) => {
        document.getElementById('error-' + name).textContent = '';
    });
}

function showFieldErrors(fieldErrors) {
    Object.keys(fieldErrors).forEach((name) => {
        const errorElement = document.getElementById('error-' + name);
        if (errorElement) {
            errorElement.textContent = fieldErrors[name];
        }
    });
}

function showList() {
    formView.hidden = true;
    listView.hidden = false;
    loadEmployees();
}

function showCurrentFile(employee) {
    const hasFile = employee !== null && employee.fileUrl;
    currentFile.textContent = '';
    currentFile.hidden = !hasFile;
    if (hasFile) {
        currentFile.append('Current file: ', createFileLinks(employee));
    }
}

function showForm(employee) {
    clearErrors();
    fieldNames.forEach((name) => {
        document.getElementById(name).value = employee ? employee[name] || '' : '';
    });
    fileInput.value = '';
    showCurrentFile(employee);
    editingId = employee ? employee.id : null;
    formTitle.textContent = employee ? 'Edit Employee' : 'Add Employee';
    listView.hidden = true;
    formView.hidden = false;
}

async function editEmployee(id) {
    try {
        const response = await fetch(API_URL + '/' + id);
        if (!response.ok) {
            const error = await readError(response);
            showMessage(listMessage, error.message);
            return;
        }
        showForm(await response.json());
    } catch {
        showMessage(listMessage, SERVER_DOWN_MESSAGE);
    }
}

function confirmDelete(employee) {
    openConfirmModal(
        'Delete employee',
        'Are you sure you want to delete ' + employee.fullName + '? This cannot be undone.',
        'Delete',
        'btn-danger',
        () => deleteEmployee(employee)
    );
}

async function deleteEmployee(employee) {
    try {
        const response = await fetch(API_URL + '/' + employee.id, { method: 'DELETE' });
        if (!response.ok) {
            const error = await readError(response);
            showMessage(listMessage, error.message);
            return;
        }
        loadEmployees();
        showSuccess('Employee deleted successfully.');
    } catch {
        showMessage(listMessage, SERVER_DOWN_MESSAGE);
    }
}

function getFileError(file) {
    if (!allowedFileTypes.includes(file.type)) {
        return 'Only PDF and image files are allowed';
    }
    if (file.size > maxFileSize) {
        return 'File size must not exceed 5MB';
    }
    return '';
}

function confirmSave(event) {
    event.preventDefault();
    fileError.textContent = '';
    const file = fileInput.files[0];
    if (file) {
        const message = getFileError(file);
        if (message !== '') {
            fileError.textContent = message;
            return;
        }
    }
    if (editingId === null) {
        openConfirmModal('Create employee', 'Are you sure you want to create this employee?', 'Create', 'btn-primary', saveEmployee);
    } else {
        openConfirmModal('Update employee', 'Are you sure you want to save the changes to this employee?', 'Update', 'btn-primary', saveEmployee);
    }
}

async function uploadFile(id, file) {
    const formData = new FormData();
    formData.append('file', file);
    try {
        const response = await fetch(uploadUrl(id), { method: 'POST', body: formData });
        if (response.ok) {
            return true;
        }
        const error = await readError(response);
        fileError.textContent = error.message;
    } catch {
        fileError.textContent = SERVER_DOWN_MESSAGE;
    }
    return false;
}

async function saveEmployee() {
    clearErrors();

    const data = {};
    fieldNames.forEach((name) => {
        data[name] = document.getElementById(name).value;
    });
    data.joiningDate = data.joiningDate || null;

    const isEdit = editingId !== null;
    const url = isEdit ? API_URL + '/' + editingId : API_URL;
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            const savedEmployee = await response.json();
            let successText = isEdit ? 'Employee updated successfully.' : 'Employee created successfully.';
            const file = fileInput.files[0];
            if (file) {
                const uploaded = await uploadFile(savedEmployee.id, file);
                if (!uploaded) {
                    editingId = savedEmployee.id;
                    formTitle.textContent = 'Edit Employee';
                    showMessage(formMessage, 'The employee was saved, but the file was not uploaded.');
                    return;
                }
                successText += ' File uploaded successfully.';
            }
            showList();
            showSuccess(successText);
            return;
        }

        const error = await readError(response);
        if (response.status === 400 && error.fieldErrors) {
            showFieldErrors(error.fieldErrors);
        } else if (response.status === 409) {
            showFieldErrors({ email: error.message });
        } else {
            showMessage(formMessage, error.message);
        }
    } catch {
        showMessage(formMessage, SERVER_DOWN_MESSAGE);
    }
}

document.getElementById('add-button').addEventListener('click', () => showForm(null));
document.getElementById('cancel-button').addEventListener('click', showList);
employeeForm.addEventListener('submit', confirmSave);
confirmOkButton.addEventListener('click', runPendingAction);
confirmCancelButton.addEventListener('click', closeConfirmModal);

confirmModal.addEventListener('click', (event) => {
    if (event.target === confirmModal) {
        closeConfirmModal();
    }
});

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !confirmModal.hidden) {
        closeConfirmModal();
    }
});

loadEmployees();