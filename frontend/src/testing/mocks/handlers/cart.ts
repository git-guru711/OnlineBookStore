import { http, HttpResponse } from 'msw';
import { aCart } from '@/testing/data-generators';
import { API } from '@/testing/mocks/api-url';

export const cartHandlers = [http.get(`${API}/cart`, () => HttpResponse.json(aCart([])))];
