const pepewLogoUrl = new URL("../../app/src/main/res/drawable/pepew_logo.png", import.meta.url).href;

type WalletHeaderProps = {
  apiState: "CONNECTED" | "READY" | "FAILED";
  versionLabel?: string;
};

export function WalletHeader({ apiState, versionLabel = "v1.0-Full" }: WalletHeaderProps) {
  const apiClass = apiState === "READY"
    ? "bg-white text-green-700"
    : apiState === "FAILED"
      ? "bg-red-50 text-red-700"
      : "bg-slate-100 text-slate-500";

  return (
    <div className="flex items-center justify-between py-2">
      <div className="flex items-center gap-2">
        <img
          src={pepewLogoUrl}
          alt="PEPEW Wallet logo"
          className="h-7 w-7 rounded-full object-cover shadow-sm"
        />
        <div className="font-mono text-xs font-black tracking-widest text-green-800">
          PEPEW WALLET <span className="rounded bg-green-50 px-2 py-1 text-[10px]">{versionLabel}</span>
        </div>
      </div>
      <div className={`rounded-full px-4 py-2 font-mono text-xs font-bold shadow-sm ${apiClass}`}>
        {apiState}
      </div>
    </div>
  );
}
