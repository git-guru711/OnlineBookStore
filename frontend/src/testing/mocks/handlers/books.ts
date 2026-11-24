import { http, HttpResponse } from 'msw';
import { aBook, aPage } from '@/testing/data-generators';
import { API } from '@/testing/mocks/api-url';

export const booksHandlers = [http.get(`${API}/books`, () => HttpResponse.json(aPage([aBook()])))];
