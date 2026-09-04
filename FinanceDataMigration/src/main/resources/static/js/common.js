function getContextPath() {
    return window.contextPath || '';
}


/*
 * Create global toast once
 */
function createGlobalToast() {

    if (document.getElementById('globalErrorToast')) {
        return;
    }

    const toast = document.createElement('div');

    toast.id = 'globalErrorToast';
    toast.className = 'global-toast error';

    toast.innerHTML = `
        <div class="global-toast-icon">!</div>

        <div class="global-toast-content">
            <div class="global-toast-title">
                Error
            </div>

            <div class="global-toast-message" id="globalToastMessage">
            </div>
        </div>

        <button
            type="button"
            class="global-toast-close"
            onclick="hideGlobalError()">
            &times;
        </button>
    `;

    document.body.appendChild(toast);
}


/*
 * Show error
 */
function showGlobalError(message, title) {

    createGlobalToast();

    const toast = document.getElementById('globalErrorToast');
    const messageElement = document.getElementById('globalToastMessage');
    const titleElement = toast.querySelector('.global-toast-title');

    titleElement.textContent = title || 'Error';

    messageElement.textContent =
        message || 'Something went wrong.';

    toast.classList.add('show');

    clearTimeout(window.globalToastTimer);

    window.globalToastTimer = setTimeout(function () {
        hideGlobalError();
    }, 5000);
}


/*
 * Hide error
 */
function hideGlobalError() {

    const toast = document.getElementById('globalErrorToast');

    if (toast) {
        toast.classList.remove('show');
    }
}


/*
 * Read error response from backend
 */
async function parseApiError(response) {

    try {

        const data = await response.json();

        return {
            message: data.message || 'Something went wrong.',
            title: data.error || 'Error'
        };

    } catch (e) {

        return {
            message: 'Unable to process server response.',
            title: 'Server Error'
        };
    }
}


/*
 * Common fetch wrapper
 *
 * Use this instead of fetch() throughout the application.
 */
async function apiFetch(url, options = {}) {

    try {

        const response = await fetch(url, options);

        if (!response.ok) {

            const error = await parseApiError(response);

            showGlobalError(
                error.message,
                error.title
            );

            throw new Error(error.message);
        }

        return response;

    } catch (error) {

        /*
         * If the error was already displayed above,
         * don't display it again.
         */
        if (error.message) {
            throw error;
        }

        showGlobalError(
            'Unable to connect to the server.',
            'Connection Error'
        );

        throw error;
    }
}

apiFetch(
    getContextPath() + '/migration/process',
    {
        method: 'POST',
        body: formData
    }
)
.then(function(response) {
    return response.json();
})
.then(function(data) {

    console.log('Migration started:', data);

    /*
     * Start checking the job
     */
    monitorMigration(data.jobId);

})
.catch(function(error) {

    console.error(
        'Migration start failed:',
        error
    );

});

function monitorMigration(jobId) {

    const interval = setInterval(async function () {

        try {

            const response = await fetch(
                getContextPath() +
                '/migration/progress/' +
                encodeURIComponent(jobId)
            );

            if (!response.ok) {
                return;
            }

            const progress = await response.json();

            console.log('Migration progress:', progress);

            /*
             * Update progress UI if you have it
             */
            if (typeof updateMigrationProgress === 'function') {
                updateMigrationProgress(progress);
            }

            /*
             * Migration completed
             */
            if (progress.status === 'SUCCESS') {

                clearInterval(interval);

                showGlobalSuccess(
                    progress.currentMessage ||
                    'Migration completed successfully.'
                );
            }

            /*
             * Migration failed
             */
            else if (progress.status === 'FAILED') {

                clearInterval(interval);

                showGlobalError(
                    progress.currentMessage ||
                    'Migration failed.',
                    'Migration Failed'
                );
            }

        } catch (error) {

            console.error(
                'Error while checking migration progress:',
                error
            );
        }

    }, 2000);
}


