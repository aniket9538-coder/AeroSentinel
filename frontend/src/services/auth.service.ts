import apiClient from './api';

export const authService = {
  login: async (credentials: { email: string; password?: string }): Promise<{ token: string; user: any }> => {
    const response = await apiClient.post('/auth/login', credentials);
    if (response.data?.token) {
      localStorage.setItem('aerosentinel_jwt', response.data.token);
    }
    return response.data;
  },
  logout: () => {
    localStorage.removeItem('aerosentinel_jwt');
  },
  getCurrentUser: () => {
    return { name: 'Demo Analyst', role: 'ANALYST' };
  }
};
