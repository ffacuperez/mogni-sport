export default function Toast({ message, isVisible, isError }) {
  if (!isVisible) return null;
  return (
    <div className={`fixed bottom-4 right-4 text-white px-6 py-3 rounded-lg shadow-lg z-50 flex items-center gap-2 animate-in slide-in-from-bottom-5 fade-in duration-300 ${isError ? 'bg-red-600' : 'bg-[#1b5e3a]'}`}>
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        {isError ? (
          <>
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </>
        ) : (
          <>
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
          </>
        )}
      </svg>
      <span className="font-medium">{message}</span>
    </div>
  );
}
