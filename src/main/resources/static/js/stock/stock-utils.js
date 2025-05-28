// ===== stock-utils.js =====
const StockUtils = {
    
    formatNumber(num) {
        if (num == null) return '0';
        return new Intl.NumberFormat('ko-KR').format(num);
    },
    
    formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            return new Date(dateStr).toLocaleDateString('ko-KR');
        } catch (error) {
            return '-';
        }
    },
    
    formatDateTime(dateStr) {
        if (!dateStr) return '-';
        try {
            return new Date(dateStr).toLocaleString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            return '-';
        }
    },
    
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
    
    showSuccess(message) {
        this.showToast(message, 'success');
    },
    
    showError(message) {
        this.showToast(message, 'danger');
    },
    
    showInfo(message) {
        this.showToast(message, 'info');
    },
    
    showWarning(message) {
        this.showToast(message, 'warning');
    },
    
    showToast(message, type = 'info') {
        const toastHtml = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        const toastContainer = this.getToastContainer();
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = toastContainer.lastElementChild;
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 3000
        });
        toast.show();
        
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    },
    
    getToastContainer() {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(container);
        }
        return container;
    },
    
    confirm(message, onConfirm, onCancel) {
        if (window.confirm(message)) {
            if (onConfirm) onConfirm();
        } else {
            if (onCancel) onCancel();
        }
    },
    
    showModal(title, body, footer = '') {
        const modalId = 'customModal_' + Date.now();
        const modalHtml = `
            <div class="modal fade" id="${modalId}" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">${title}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">${body}</div>
                        ${footer ? `<div class="modal-footer">${footer}</div>` : ''}
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalElement = document.getElementById(modalId);
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
        
        modalElement.addEventListener('hidden.bs.modal', () => {
            modalElement.remove();
        });
        
        return modal;
    },
    
    showLoading(targetElement) {
        if (targetElement) {
            targetElement.innerHTML = `
                <div class="text-center py-5">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            `;
        }
    },
    
    handleApiError(error, defaultMessage = '요청 처리에 실패했습니다.') {
        console.error('API Error:', error);
        
        let message = defaultMessage;
        if (error.response && error.response.data && error.response.data.message) {
            message = error.response.data.message;
        } else if (error.message) {
            message = error.message;
        }
        
        this.showError(message);
    },
    
    formToObject(formElement) {
        const formData = new FormData(formElement);
        const object = {};
        formData.forEach((value, key) => {
            object[key] = value;
        });
        return object;
    },
    
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    storage: {
        set(key, value) {
            try {
                localStorage.setItem(key, JSON.stringify(value));
            } catch (e) {
                console.error('LocalStorage error:', e);
            }
        },
        
        get(key) {
            try {
                const item = localStorage.getItem(key);
                return item ? JSON.parse(item) : null;
            } catch (e) {
                console.error('LocalStorage error:', e);
                return null;
            }
        },
        
        remove(key) {
            try {
                localStorage.removeItem(key);
            } catch (e) {
                console.error('LocalStorage error:', e);
            }
        }
    }
};
