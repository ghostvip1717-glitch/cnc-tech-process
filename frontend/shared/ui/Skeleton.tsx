import "./ui.css";

interface SkeletonProps {
  height?: number | string;
  width?: number | string;
  className?: string;
}

export function Skeleton({ height = "1rem", width = "100%", className = "" }: SkeletonProps) {
  return (
    <div
      className={`ui-skeleton ${className}`.trim()}
      style={{ height, width }}
      aria-hidden
    />
  );
}

export function SkeletonStack({
  rows = 3,
  rowHeight = "4.5rem",
}: {
  rows?: number;
  rowHeight?: string;
}) {
  return (
    <div className="ui-skeleton-stack" aria-busy="true" aria-label="Загрузка">
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} height={rowHeight} />
      ))}
    </div>
  );
}
