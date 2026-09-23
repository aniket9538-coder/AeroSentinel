import React, { useState } from 'react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { ImageUploader } from './ImageUploader';
import { MapPin } from 'lucide-react';

interface ReportFormProps {
  onSubmit: (formData: FormData) => void;
  isSubmitting?: boolean;
}

export const ReportForm: React.FC<ReportFormProps> = ({ onSubmit, isSubmitting = false }) => {
  const [category, setCategory] = useState<'SMOKE' | 'DUST' | 'BURNING' | 'ODOR' | 'OTHER'>('SMOKE');
  const [description, setDescription] = useState('');
  const [coords, setCoords] = useState<{ lat: number; lng: number }>({ lat: 18.5204, lng: 73.8567 });
  const [imageFile, setImageFile] = useState<File | null>(null);

  const handleUseLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((pos) => {
        setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude });
      });
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const data = new FormData();
    data.append('category', category);
    data.append('description', description);
    data.append('latitude', coords.lat.toString());
    data.append('longitude', coords.lng.toString());
    if (imageFile) {
      data.append('image', imageFile);
    }
    onSubmit(data);
  };

  return (
    <Card title="Report Air Pollution Incident" subtitle="Submit crowdsourced ground evidence to municipal monitoring">
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '1rem' }}>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
            Pollution Category
          </label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value as any)}
            style={{
              width: '100%',
              padding: '0.6rem',
              backgroundColor: '#1f2937',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '6px',
              color: '#f3f4f6',
              fontSize: '0.875rem',
            }}
          >
            <option value="SMOKE">Dense Black/Grey Smoke</option>
            <option value="BURNING">Waste or Agricultural Burning</option>
            <option value="DUST">Road or Construction Dust</option>
            <option value="ODOR">Chemical or Industrial Odor</option>
            <option value="OTHER">Other Atmospheric Emission</option>
          </select>
        </div>

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
            <label style={{ fontSize: '0.85rem', color: '#9ca3af' }}>Location Coordinates</label>
            <button
              type="button"
              onClick={handleUseLocation}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#38bdf8',
                fontSize: '0.75rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.25rem',
              }}
            >
              <MapPin size={12} /> Use GPS
            </button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <input
              type="number"
              step="any"
              value={coords.lat}
              onChange={(e) => setCoords({ ...coords, lat: parseFloat(e.target.value) })}
              placeholder="Latitude"
              style={{
                padding: '0.5rem',
                backgroundColor: '#1f2937',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '6px',
                color: '#f3f4f6',
                fontSize: '0.85rem',
              }}
            />
            <input
              type="number"
              step="any"
              value={coords.lng}
              onChange={(e) => setCoords({ ...coords, lng: parseFloat(e.target.value) })}
              placeholder="Longitude"
              style={{
                padding: '0.5rem',
                backgroundColor: '#1f2937',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '6px',
                color: '#f3f4f6',
                fontSize: '0.85rem',
              }}
            />
          </div>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
            Description of Incident
          </label>
          <textarea
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe what you see, e.g., burning pile behind industrial warehouse..."
            style={{
              width: '100%',
              padding: '0.6rem',
              backgroundColor: '#1f2937',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '6px',
              color: '#f3f4f6',
              fontSize: '0.875rem',
              resize: 'vertical',
            }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: '#9ca3af', marginBottom: '0.35rem' }}>
            Visual Evidence (Photo)
          </label>
          <ImageUploader onImageSelected={(f) => setImageFile(f)} />
        </div>

        <Button type="submit" variant="primary" isLoading={isSubmitting}>
          Submit Report to Environmental Authority
        </Button>
      </form>
    </Card>
  );
};
