export function PepewLogoBadge() {
  return (
    <div className="fixed left-3 top-3 z-[70] flex items-center gap-2 rounded-full bg-white/90 px-2 py-1 shadow-sm backdrop-blur">
      <img
        src="/app/public/pepew_logo.png"
        alt="PEPEW Wallet logo"
        className="h-6 w-6 rounded-full object-cover"
      />
      <span className="hidden font-mono text-[10px] font-black tracking-widest text-green-800 sm:inline">
        PEPEW
      </span>
    </div>
  );
}
