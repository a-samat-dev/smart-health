export default function FormField({ label, ...inputProps }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <input className="field-input" {...inputProps} />
    </label>
  );
}
