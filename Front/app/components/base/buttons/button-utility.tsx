import type { FC, SVGProps, ReactNode } from "react";
import { Button } from "./button";

interface ButtonUtilityProps {
  size?: "sm" | "md" | "lg";
  color?: "primary" | "tertiary";
  tooltip?: string;
  icon?: FC<SVGProps<SVGSVGElement>>;
  iconLeading?: ReactNode;
  onClick?: () => void;
  children?: ReactNode;
  className?: string;
}

export const ButtonUtility: FC<ButtonUtilityProps> = ({
  size = "md",
  color = "primary",
  tooltip,
  icon: Icon,
  iconLeading,
  onClick,
  children,
  className,
}) => {
  return (
    <Button
      size={size}
      color={color}
      onClick={onClick}
      title={tooltip}
      className={className}
    >
      {iconLeading || (Icon ? <Icon className="size-4" /> : null)}
      {children}
    </Button>
  );
};
