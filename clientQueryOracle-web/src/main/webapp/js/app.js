// Global state
let hasActiveTransaction = false;
let currentUser = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', async function() {
    console.log('Oracle Query Client initialized');

    // Check authentication first
    const authenticated = await checkAuthentication();
    if (!authenticated) {
        // Redirect to login page
        window.location.href = 'login.html';
        return;
    }

    // Initialize application
    loadAliases();
    checkTransactionStatus();
});

/**
 * Load available database aliases from server
 */
function loadAliases() {
    fetch('api/query/aliases')
        .then(response => response.json())
        .then(data => {
            if (data.success && data.aliases) {
                const select = document.getElementById('aliasSelect');
                // Keep the first option, clear the rest
                select.innerHTML = '<option value="">-- Auto-detect from query --</option>';

                data.aliases.forEach(alias => {
                    const option = document.createElement('option');
                    option.value = alias.alias;
                    option.textContent = alias.alias + ' (' + alias.jndiName + ')';
                    select.appendChild(option);
                });
            }
        })
        .catch(error => {
            console.error('Error loading aliases:', error);
        });
}

/**
 * Check transaction status and update session ID
 */
function checkTransactionStatus() {
    fetch('api/query/status')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                hasActiveTransaction = data.hasActiveTransaction;

                if (data.sessionId) {
                    document.getElementById('sessionId').textContent = data.sessionId;
                }

                updateTransactionUI();
            }
        })
        .catch(error => {
            console.error('Error checking status:', error);
        });
}

/**
 * Execute SQL query
 */
function executeQuery() {
    const alias = document.getElementById('aliasSelect').value;
    const query = document.getElementById('queryInput').value.trim();

    // Validate input
    if (!query) {
        alert('Please enter a SQL query');
        return;
    }

    // Note: alias is optional - it will be extracted from query if not provided

    // Disable buttons and show loading
    setLoadingState(true);

    // Prepare request
    const requestData = {
        alias: alias,
        query: query
    };

    // Send request
    fetch('api/query/execute', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        if (response.status === 401) {
            // Not authenticated - redirect to login
            alert('Session expired. Please login again.');
            window.location.href = 'login.html';
            return null;
        }
        return response.json();
    })
    .then(data => {
        if (data) {
            setLoadingState(false);
            displayResults(data);

            if (data.success) {
                hasActiveTransaction = true;
                updateTransactionUI();
            }
        }
    })
    .catch(error => {
        setLoadingState(false);
        displayError('Network error: ' + error.message);
    });
}

/**
 * Commit transaction
 */
function commitTransaction() {
    showConfirmModal(
        'Confirm Commit',
        'Are you sure you want to commit these changes? This action cannot be undone.',
        'Commit',
        'success',
        function() {
            setLoadingState(true);

            fetch('api/query/commit', {
                method: 'POST'
            })
            .then(response => {
                if (response.status === 401) {
                    showAlert('Session expired. Please login again.', 'error');
                    setTimeout(() => window.location.href = 'login.html', 2000);
                    return null;
                }
                return response.json();
            })
            .then(data => {
                if (data) {
                    setLoadingState(false);
                    displayResults(data);

                    if (data.success) {
                        hasActiveTransaction = false;
                        updateTransactionUI();
                        showAlert('Transaction committed successfully!', 'success');
                    }
                }
            })
            .catch(error => {
                setLoadingState(false);
                displayError('Network error: ' + error.message);
            });
        }
    );
}

/**
 * Rollback transaction
 */
function rollbackTransaction() {
    showConfirmModal(
        'Confirm Rollback',
        'Are you sure you want to rollback? All changes will be discarded.',
        'Rollback',
        'danger',
        function() {
            setLoadingState(true);

            fetch('api/query/rollback', {
                method: 'POST'
            })
            .then(response => {
                if (response.status === 401) {
                    showAlert('Session expired. Please login again.', 'error');
                    setTimeout(() => window.location.href = 'login.html', 2000);
                    return null;
                }
                return response.json();
            })
            .then(data => {
                if (data) {
                    setLoadingState(false);
                    displayResults(data);

                    if (data.success) {
                        hasActiveTransaction = false;
                        updateTransactionUI();
                        showAlert('Transaction rolled back successfully!', 'info');
                    }
                }
            })
            .catch(error => {
                setLoadingState(false);
                displayError('Network error: ' + error.message);
            });
        }
    );
}

/**
 * Display query results
 */
function displayResults(data) {
    const resultsSection = document.getElementById('resultsSection');
    const statusText = document.getElementById('statusText');
    const affectedRows = document.getElementById('affectedRows');
    const messageText = document.getElementById('messageText');
    const errorDetailsRow = document.getElementById('errorDetailsRow');
    const errorDetails = document.getElementById('errorDetails');

    // Show results section
    resultsSection.style.display = 'block';

    // Update status
    if (data.success) {
        statusText.textContent = 'SUCCESS';
        statusText.className = 'status-badge success';
    } else {
        statusText.textContent = 'ERROR';
        statusText.className = 'status-badge error';
    }

    // Update affected rows with detail if available
    if (data.affectedRowsDetail) {
        // Format: "1 - 2 - 1 TOTALE: 4"
        affectedRows.textContent = data.affectedRowsDetail + ' TOTALE: ' + (data.affectedRows || 0);
    } else {
        affectedRows.textContent = data.affectedRows || 0;
    }

    // Update message
    messageText.textContent = data.message || 'No message';

    // Update error details if present
    if (data.errorDetails) {
        errorDetailsRow.style.display = 'flex';
        errorDetails.textContent = data.errorDetails;
    } else {
        errorDetailsRow.style.display = 'none';
    }

    // Scroll to results
    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * Display error message
 */
function displayError(message) {
    displayResults({
        success: false,
        affectedRows: 0,
        message: message,
        errorDetails: null
    });
}

/**
 * Update transaction control UI
 */
function updateTransactionUI() {
    const transactionButtons = document.getElementById('transactionButtons');

    if (hasActiveTransaction) {
        transactionButtons.style.display = 'flex';
    } else {
        transactionButtons.style.display = 'none';
    }
}

/**
 * Set loading state
 */
function setLoadingState(isLoading) {
    const loadingIndicator = document.getElementById('loadingIndicator');
    const executeBtn = document.getElementById('executeBtn');
    const commitBtn = document.getElementById('commitBtn');
    const rollbackBtn = document.getElementById('rollbackBtn');

    if (isLoading) {
        loadingIndicator.style.display = 'flex';
        executeBtn.disabled = true;
        if (commitBtn) commitBtn.disabled = true;
        if (rollbackBtn) rollbackBtn.disabled = true;
    } else {
        loadingIndicator.style.display = 'none';
        executeBtn.disabled = false;
        if (commitBtn) commitBtn.disabled = false;
        if (rollbackBtn) rollbackBtn.disabled = false;
    }
}

/**
 * Clear query input
 */
function clearQuery() {
    if (hasActiveTransaction) {
        if (!confirm('You have an active transaction. Clearing the query will not affect it. Continue?')) {
            return;
        }
    }

    document.getElementById('queryInput').value = '';
    document.getElementById('aliasSelect').selectedIndex = 0;
}

/**
 * Check if user is authenticated
 */
async function checkAuthentication() {
    try {
        const response = await fetch('api/auth/check');
        const data = await response.json();

        if (data.success && data.authenticated) {
            currentUser = {
                username: data.username,
                displayName: data.displayName
            };

            // Show user info
            document.getElementById('userDisplay').textContent = data.displayName;
            document.getElementById('userInfo').style.display = 'block';

            return true;
        }

        return false;

    } catch (error) {
        console.error('Authentication check error:', error);
        return false;
    }
}

/**
 * Logout user
 */
async function logout() {
    if (hasActiveTransaction) {
        showConfirmModal(
            'Confirm Logout',
            'You have an active transaction that will be rolled back. Continue with logout?',
            'Logout',
            'danger',
            async function() {
                await performLogout();
            }
        );
    } else {
        await performLogout();
    }
}

/**
 * Perform the actual logout
 */
async function performLogout() {
    try {
        const response = await fetch('api/auth/logout', {
            method: 'POST'
        });

        const data = await response.json();

        if (data.success) {
            window.location.href = 'login.html';
        } else {
            showAlert('Logout failed: ' + data.message, 'error');
        }

    } catch (error) {
        console.error('Logout error:', error);
        showAlert('Error during logout', 'error');
    }
}

/**
 * Show custom confirmation modal
 */
function showConfirmModal(title, message, confirmText, type, onConfirm) {
    const modal = document.getElementById('confirmModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalMessage = document.getElementById('modalMessage');
    const modalConfirmBtn = document.getElementById('modalConfirmBtn');
    const modalCancelBtn = document.getElementById('modalCancelBtn');

    modalTitle.textContent = title;
    modalMessage.textContent = message;
    modalConfirmBtn.textContent = confirmText;

    // Set button color based on type
    modalConfirmBtn.className = 'modal-btn modal-btn-' + type;

    // Show modal
    modal.style.display = 'flex';

    // Handle confirm
    modalConfirmBtn.onclick = function() {
        modal.style.display = 'none';
        if (onConfirm) onConfirm();
    };

    // Handle cancel
    modalCancelBtn.onclick = function() {
        modal.style.display = 'none';
    };

    // Close on overlay click
    modal.onclick = function(event) {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    };
}

/**
 * Show alert notification
 */
function showAlert(message, type) {
    const alert = document.getElementById('alertNotification');
    const alertMessage = document.getElementById('alertMessage');
    const alertIcon = document.getElementById('alertIcon');

    alertMessage.textContent = message;
    alert.className = 'alert-notification alert-' + type;

    // Set icon based on type
    if (type === 'success') {
        alertIcon.textContent = '✓';
    } else if (type === 'error') {
        alertIcon.textContent = '✕';
    } else if (type === 'info') {
        alertIcon.textContent = 'ℹ';
    }

    alert.style.display = 'flex';

    // Auto-hide after 3 seconds
    setTimeout(() => {
        alert.style.display = 'none';
    }, 3000);
}
