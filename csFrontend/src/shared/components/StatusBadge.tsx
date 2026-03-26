interface StatusBadgeProps {
  label: string;
  tone?: "neutral" | "blue" | "green" | "orange" | "red" | "purple";
}
export function StatusBadge({ label, tone = "neutral" }: StatusBadgeProps) {
  return <span className={`badge badge--${tone}`}>{label}</span>;
}
