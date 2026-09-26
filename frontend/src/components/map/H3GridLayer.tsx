import React from 'react';
import { Polygon, Popup } from 'react-leaflet';
import { GridCellResponse, GridCellObservationResponse } from '../../types/grid';
import { H3CellPopup } from './H3CellPopup';

interface H3GridLayerProps {
  cells: GridCellResponse[];
  selectedH3Index?: string | null;
  onSelectCell?: (cell: GridCellResponse) => void;
  selectedCellObservations?: GridCellObservationResponse | null;
  isLoadingObservations?: boolean;
}

export const H3GridLayer: React.FC<H3GridLayerProps> = ({
  cells = [],
  selectedH3Index,
  onSelectCell,
  selectedCellObservations,
  isLoadingObservations = false,
}) => {
  return (
    <>
      {cells.map((cell) => {
        // Construct Leaflet coordinates strictly from backend-authoritative boundary vertices
        const positions: [number, number][] = cell.boundary && cell.boundary.length > 0
          ? cell.boundary.map((pt) => [pt.lat, pt.lng])
          : [];

        if (positions.length === 0) {
          return null;
        }

        const isSelected = selectedH3Index === cell.h3Index;

        // Neutral, elegant spatial styling (F2 has spatial boundaries without speculative risk tags)
        const pathOptions = {
          color: isSelected ? '#0284c7' : '#0ea5e9',
          fillColor: isSelected ? '#38bdf8' : '#0284c7',
          fillOpacity: isSelected ? 0.35 : 0.15,
          weight: isSelected ? 2.5 : 1.2,
          dashArray: isSelected ? undefined : '3, 3',
        };

        return (
          <Polygon
            key={cell.h3Index}
            positions={positions}
            pathOptions={pathOptions}
            eventHandlers={{
              click: () => onSelectCell?.(cell),
            }}
          >
            <Popup>
              <H3CellPopup
                cell={cell}
                observations={isSelected ? selectedCellObservations : null}
                isLoadingObservations={isSelected ? isLoadingObservations : false}
              />
            </Popup>
          </Polygon>
        );
      })}
    </>
  );
};

export default H3GridLayer;
