import { IconPlus } from "./Icons";
import "./ui.css";

interface FabProps {
  label: string;
  onClick: () => void;
  disabled?: boolean;
}

export function Fab({ label, onClick, disabled = false }: FabProps) {
  return (
    <button
      className="ui-fab"
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      disabled={disabled}
    >
      <IconPlus size={24} />
    </button>
  );
}
