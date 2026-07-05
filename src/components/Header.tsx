import { ArrowLeft } from "lucide-react";

type HeaderProps = {
  title: string;
  onBack?: () => void;
};

export function Header(props: HeaderProps) {
  return (
    <div className="sticky top-0 z-10 flex items-center gap-3 border-b border-green-100 bg-white px-4 py-3 text-green-800">
      {props.onBack ? (
        <button onClick={props.onBack} className="rounded-full p-1 hover:bg-green-50">
          <ArrowLeft size={18} />
        </button>
      ) : null}
      <div className="font-mono text-sm font-bold tracking-[0.25em]">{props.title}</div>
    </div>
  );
}
