import { useEffect, useRef } from 'react';

/** A modal dialog, shown while open is true. Escape and the close button call onClose. */
export default function Dialog({ open, title, onClose, children }) {
  const ref = useRef(null);

  useEffect(() => {
    const dialog = ref.current;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog ref={ref} className="dialog" onClose={onClose} aria-labelledby="dialog-title">
      {open && (
        <>
          <div className="dialog-header">
            <h2 id="dialog-title">{title}</h2>
            <button type="button" className="icon-button" onClick={onClose} aria-label="Close">×</button>
          </div>
          {children}
        </>
      )}
    </dialog>
  );
}
