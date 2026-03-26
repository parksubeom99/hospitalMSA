"use client";

import { ringColorByLevel } from "@/shared/lib/capacity";
import type { CapacityLevel } from "@/shared/types/domain";

interface Donut3DProps {
  waitingAndInTreatment: number;
  reservation: number;
  emergency: number;
  max: number;
  current: number;
  level: CapacityLevel;
}

export function Donut3D(props: Donut3DProps) {
  const { waitingAndInTreatment, reservation, emergency, current, max, level } = props;
  const total = Math.max(waitingAndInTreatment + reservation + emergency, 1);
  const a = (waitingAndInTreatment / total) * 360;
  const b = (reservation / total) * 360;
  const c1 = "#8ad8ff";
  const c2 = "#6aa2ff";
  const c3 = "#ff7f8f";
  const bg = "#1e293b";
  const gradient = `conic-gradient(from -90deg, ${c1} 0deg ${a.toFixed(2)}deg, ${c2} ${a.toFixed(2)}deg ${(a+b).toFixed(2)}deg, ${c3} ${(a+b).toFixed(2)}deg 360deg)`;
  const levelColor = ringColorByLevel(level);
  const percent = Math.round((current / max) * 100);

  return (
    <div className={`donut3d donut3d--${level.toLowerCase()}`}>
      <div className="donut3d__shadow" />
      <div className="donut3d__ring" style={{ background: gradient, ["--ring-gradient" as any]: gradient }}>
        <div className="donut3d__inner">
          <div className="donut3d__label">총 운영 인원</div>
          <div className="donut3d__value">{current}<span> / {max}</span></div>
          <div className="donut3d__percent" style={{ color: levelColor }}>{percent}%</div>
        </div>
      </div>
      <div className="donut3d__legend">
        <span><i style={{ background: c1 }} />대기+진료중</span>
        <span><i style={{ background: c2 }} />예약</span>
        <span><i style={{ background: c3 }} />응급</span>
      </div>
    </div>
  );
}
