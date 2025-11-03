import classes from './Modal.module.scss'

export default function Modal({ children, title, onClose }) {

    return <>
        <div className={classes.overlay} onClick={onClose} />
        <div className={classes.modal}>
            {title && <p className={`${classes.title} h5 d-flex justify-content-between align-items-center`}>
                {title}
                <button className='btn btn-none' onClick={onClose}>
                    <i className='fas fa-times' />
                </button>
            </p>}
            <div className={classes.body}>
                {children}
            </div>
        </div>
    </>
}