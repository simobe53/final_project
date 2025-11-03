export default function BaseModal({ title, onClose, children, showCloseButton = true }) {
    return (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered">
                <div className="modal-content border-radius-20">
                    <div className="modal-header border-0">
                        <h5 className="modal-title w-100 text-center">{title}</h5>
                        {showCloseButton && (
                            <button
                                type="button"
                                className="btn-close"
                                onClick={onClose}
                            ></button>
                        )}
                    </div>
                    {children}
                </div>
            </div>
        </div>
    );
}
