interface CodeShiftLogoProps {
  className?: string;
}

export default function CodeShiftLogo({ className = "" }: CodeShiftLogoProps) {
  return (
    <svg
      className={className}
      viewBox="0 0 64 64"
      role="img"
      aria-label="CodeShift logo"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <linearGradient id="codeshift-grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="var(--accent-strong)" />
          <stop offset="100%" stopColor="var(--accent)" />
        </linearGradient>
      </defs>
      <rect x="6" y="6" width="52" height="52" rx="16" className="logo-track" />
      <path d="M22 23h20l-7 7h-13v-7z" className="logo-shape" />
      <path d="M42 41H22l7-7h13v7z" className="logo-shape" />
      <circle cx="32" cy="32" r="3" className="logo-core" />
      <rect x="6" y="6" width="52" height="52" rx="16" fill="url(#codeshift-grad)" opacity="0.12" />
    </svg>
  );
}
