export default function ErrorMessage({ error }) {
  if (!error) {
    return null;
  }
  return (
    <div className="error" role="alert">
      <p>{error.message}</p>
      {error.details?.length > 0 && (
        <ul>
          {error.details.map((detail) => <li key={detail}>{detail}</li>)}
        </ul>
      )}
    </div>
  );
}
