import React, { useState } from 'react';
import { PageContainer } from '../../components/layout/PageContainer';
import { ReportForm } from '../../components/citizen/ReportForm';
import { ReportStatus } from '../../components/citizen/ReportStatus';
import { CitizenReport as ICitizenReport } from '../../types';

export const CitizenReport: React.FC = () => {
  const [reports, setReports] = useState<ICitizenReport[]>([
    {
      id: 'r1',
      cityId: 'pune',
      latitude: 18.5204,
      longitude: 73.8567,
      category: 'SMOKE',
      description: 'Heavy black smoke rising behind MIDC chemical warehouse.',
      submittedAt: new Date(Date.now() - 3600000).toISOString(),
      status: 'VERIFIED',
    },
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (formData: FormData) => {
    setIsSubmitting(true);
    setTimeout(() => {
      const newReport: ICitizenReport = {
        id: 'r-' + Date.now(),
        cityId: 'pune',
        latitude: parseFloat(formData.get('latitude') as string),
        longitude: parseFloat(formData.get('longitude') as string),
        category: formData.get('category') as any,
        description: formData.get('description') as string,
        submittedAt: new Date().toISOString(),
        status: 'PENDING',
      };
      setReports([newReport, ...reports]);
      setIsSubmitting(false);
      alert('Report submitted successfully! Thank you for contributing to clean air intelligence.');
    }, 1000);
  };

  return (
    <PageContainer
      title="Citizen Air Quality Reporting"
      subtitle="Submit crowdsourced ground observations and visual emission evidence"
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '2rem' }}>
        <ReportForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
        <ReportStatus reports={reports} />
      </div>
    </PageContainer>
  );
};
