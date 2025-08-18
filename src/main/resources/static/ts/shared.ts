import '../scss/styles.scss';

import { Modal, Popover, Toast, Tooltip } from 'bootstrap';
import { Network, Options } from 'vis-network/peer';
import { DataSet } from 'vis-data/peer';

export { DataSet };

export const BASE_URL = `${window.location.protocol}//${window.location.host}`;

export const WIKI_BASE_URL = 'https://en.wikipedia.org';

export enum Colors {
  ENTER = '#28A745',
  PAGE = '#007BFF',
  REDIRECT = '#FFC107',
  EXIT = '#DC3545',
}

export enum Solvers {
  BARNES_HUT = 'barnesHut',
  REPULSION = 'repulsion',
  HIERARCHICAL_REPULSION = 'hierarchicalRepulsion',
  FORCE_ATLAS_2_BASED = 'forceAtlas2Based',
}

export enum Directions {
  UD = 'UD',
  DU = 'DU',
  LR = 'LR',
  RL = 'RL',
}

export const DEFAULT_IMG_SRC =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100%' height='180'%3E%3Crect width='100%' height='100%' fill='%23868e96' /%3E%3C/svg%3E";

export const MAX_NODE_SIZE: number = 100;

export class FetchError extends Error {
  status: number;
  statusText: string;
  url: string;
  body?: any | null;
  code?: string | null;

  constructor({
    message,
    status,
    statusText,
    url,
    body,
    code,
  }: {
    message: string;
    status: number;
    statusText: string;
    url: string;
    body?: any | null;
    code?: string | null;
  }) {
    super(message);
    this.status = status;
    this.statusText = statusText;
    this.url = url;
    this.name = 'FetchError';
    this.body = body;
    this.code = code;
    Object.setPrototypeOf(this, FetchError.prototype);
  }
}

export class NetworkWrapper {
  network: Network | null;

  constructor(network: Network | null) {
    this.network = network;
  }
}

const network: Network | null = null;

export const wrapper: NetworkWrapper = new NetworkWrapper(network);

export interface Node {
  id: string;
  label: string;
  title: string;
  color: string;
  type: NodeDtoType;
}

export interface Edge {
  id: string;
  from: string;
  to: string;
  arrows: string;
  label: string;
}

export interface NodeDto {
  id: string;
  title: string;
  type: NodeDtoType;
  outgoing?: NodeDto[] | null;
  incoming?: NodeDto[] | null;
  categories?: CategoryDto[] | null;
  isTopParent?: boolean | null;
  isBottomChild?: boolean | null;
}

export enum NodeDtoType {
  PAGE = 'PAGE',
  REDIRECT = 'REDIRECT',
}

export interface RelationDto {
  source: NodeDto;
  target: NodeDto;
}

export interface ThumbnailDto {
  source?: string | null;
  width?: number | null;
  height?: number | null;
}

export interface CategoryDto {
  id: string;
  title: string;
  contains?: NodeDto[] | null;
}

interface SearchSuggestionsDto {
  pages?: SearchSuggestion[] | null;
}

interface SearchSuggestion {
  id: string;
  key: string;
  title: string;
  description?: string;
  thumbnail?: ThumbnailDto;
}

export async function fetchData<T>(url: string): Promise<T | null> {
  try {
    const response = await fetch(url);
    const text = await response.text();
    let body: any;
    try {
      body = text ? JSON.parse(text) : null;
    } catch (_) {
      body = null;
    }
    if (!response.ok) {
      throw new FetchError({
        message: body?.message || 'Request failed',
        status: response.status,
        statusText: response.statusText,
        url: response.url,
        body: body,
        code: body?.code,
      });
    }
    return body as T;
  } catch (error) {
    if (error instanceof FetchError) {
      if (error.status === 429) {
        showToast({ message: 'Too many requests. Please try again later.' });
      } else {
        showToast({
          message: `error: ${error.status}<br/>code: ${error.code}<br/>message: ${error.message}<br/> url: ${error.url}`,
        });
      }
    } else {
      showToast({ message: 'An unexpected error occurred. Please try again.' });
    }
    throw error;
  }
}

export async function fetchWithUI<T>(url: string): Promise<T | null> {
  try {
    toggleSpinner({ show: true });
    setButtonState({ id: 'graph-button', enabled: false });
    setButtonState({ id: 'random-button', enabled: false });
    setButtonState({ id: 'download-button', enabled: false });
    setButtonState({ id: 'clear-button', enabled: false });
    return await fetchData<T>(url);
  } finally {
    toggleSpinner({ show: false });
    setButtonState({ id: 'graph-button' });
    setButtonState({ id: 'random-button' });
    setButtonState({ id: 'download-button' });
    setButtonState({ id: 'clear-button' });
  }
}

export async function renderNetwork({
  wrapper,
  nodes,
  edges,
  options,
  focusNodeId = null,
}: {
  wrapper: NetworkWrapper;
  nodes: DataSet<Node>;
  edges: DataSet<Edge>;
  options: Options;
  focusNodeId?: string | number | null;
}): Promise<void> {
  clearGraph(wrapper);
  toggleSpinner({ show: true, message: 'plotting' });
  setButtonState({ id: 'graph-button', enabled: false });
  setButtonState({ id: 'random-button', enabled: false });
  setButtonState({ id: 'clear-button', enabled: false });
  const container = document.getElementById('graph') as HTMLElement | null;
  if (!container) {
    console.error(`graph container not found`);
    return;
  }
  wrapper.network = new Network(
    container,
    { nodes: nodes, edges: edges },
    options
  );
  wrapper.network.on('click', async (event) => {
    const clickedNodes = event.nodes;
    if (clickedNodes?.length > 0) {
      const clickedNodeId: string = clickedNodes[0];
      const node = nodes.get(clickedNodeId);
      if (!node) {
        console.error(`node ${clickedNodeId} not found`);
        return;
      }
      let data: ThumbnailDto | null | undefined;
      if (node.type === NodeDtoType.PAGE) {
        data = await fetchWithUI(
          `${BASE_URL}/api/wiki/image?title=${encodeURIComponent(node.label)}&piThumbSize=200`
        );
      }
      const wikipediaHref = `${WIKI_BASE_URL}/wiki/${encodeURIComponent(node.label)}`;
      showModal({
        src: data?.source,
        height: data?.height,
        width: data?.width,
        alt: node.label,
        title: node.label,
        wikipediaHref,
      });
    }
  });
  wrapper.network.once('afterDrawing', () => {
    toggleSpinner({ show: false });
    setButtonState({ id: 'graph-button' });
    setButtonState({ id: 'random-button' });
    setButtonState({ id: 'clear-button' });
    if (focusNodeId) {
      wrapper?.network?.focus(focusNodeId, {
        scale: 1.5,
        animation: {
          duration: 400,
          easingFunction: 'easeInQuad',
        },
      });
    }
  });
}

export function toggleSpinner({
  show,
  message = 'fetching',
}: {
  show: boolean;
  message?: string | null;
}): void {
  const spinnerContainer = document.getElementById(
    'spinner'
  ) as HTMLElement | null;
  if (!spinnerContainer) {
    console.error('spinner container not found');
    return;
  }
  spinnerContainer.innerHTML = '';
  if (show) {
    const button = document.createElement('button');
    button.className = 'btn btn-dark';
    button.setAttribute('type', 'button');
    button.setAttribute('disabled', 'true');
    const spinner = document.createElement('span');
    spinner.className = 'spinner-border spinner-border-sm';
    spinner.setAttribute('aria-hidden', 'true');
    const status = document.createElement('span');
    status.setAttribute('role', 'status');
    status.textContent = ` ${message}...`;
    button.appendChild(spinner);
    button.appendChild(status);
    spinnerContainer.appendChild(button);
  }
}

export function clearToasts(): void {
  document.querySelectorAll('.toast').forEach((toastEl) => {
    const instance = Toast.getInstance(toastEl);
    if (instance) instance.dispose();
    toastEl.remove();
  });
}

export function showToast({
  message,
  theme = 'text-bg-danger',
}: {
  message: string;
  theme?: string | null;
}): void {
  clearToasts();
  const toastContainer = document.getElementById('toast-container');
  if (!toastContainer) {
    console.error('toast container not found');
    return;
  }
  const toast = document.createElement('div');
  toast.className = `toast align-items-center ${theme} border-0`;
  toast.setAttribute('role', 'alert');
  toast.setAttribute('aria-live', 'assertive');
  toast.setAttribute('aria-atomic', 'true');
  toast.setAttribute('data-bs-delay', '10000');
  const flexDiv = document.createElement('div');
  flexDiv.className = 'd-flex';
  const toastBody = document.createElement('div');
  toastBody.className = 'toast-body wrap p-3';
  toastBody.innerHTML = message;
  const closeButton = document.createElement('button');
  closeButton.type = 'button';
  closeButton.className = 'btn-close me-2 m-auto';
  closeButton.setAttribute('data-bs-dismiss', 'toast');
  closeButton.setAttribute('aria-label', 'Close');
  flexDiv.appendChild(toastBody);
  flexDiv.appendChild(closeButton);
  toast.appendChild(flexDiv);
  toastContainer.appendChild(toast);
  const bsToast = new Toast(toast);
  bsToast.show();
  toast.addEventListener('hidden.bs.toast', () => {
    bsToast.dispose();
    toast.remove();
  });
}

export function clearGraph(wrapper: NetworkWrapper): void {
  if (wrapper.network) {
    wrapper.network.destroy();
    wrapper.network = null;
  }
}

export function getInputValue(id: string): string {
  const input = document.getElementById(id) as HTMLInputElement | null;
  if (!input) {
    console.error(`input ${id} not found!`);
    throw new Error(`input ${id} not found!`);
  }
  return input.value.trim();
}

export function setInputValue({
  id,
  value,
}: {
  id: string;
  value: string | number;
}): void {
  const input = document.getElementById(id) as HTMLInputElement | null;
  if (!input) {
    console.error(`input ${id} not found!`);
    return;
  }
  input.value = `${value}`;
}

export function clearForm(ids: string[]): void {
  ids.forEach((id: string) => setInputValue({ id, value: '' }));
}

export function reverseInputs({
  sourceInputId,
  targetInputId,
}: {
  sourceInputId: string;
  targetInputId: string;
}): void {
  const [source, target] = [
    getInputValue(sourceInputId),
    getInputValue(targetInputId),
  ];
  setInputValue({ id: sourceInputId, value: target });
  setInputValue({ id: targetInputId, value: source });
}

export function getCheckboxState(id: string): boolean {
  const checkbox = document.getElementById(id) as HTMLInputElement | null;
  if (!checkbox) {
    console.error(`checkbox ${id} not found!`);
    throw new Error(`checkbox ${id} not found!`);
  }
  return checkbox.checked;
}

export function toggleSelect({
  id,
  enabled,
}: {
  id: string;
  enabled: boolean;
}): void {
  const select = document.getElementById(id) as HTMLSelectElement | null;
  if (!select) {
    console.error(`select ${id} not found!`);
    return;
  }
  select.disabled = !enabled;
}

export function setButtonState({
  id,
  enabled = true,
}: {
  id: string;
  enabled?: boolean | null;
}): void {
  const button = document.getElementById(id) as HTMLButtonElement | null;
  if (!button) {
    console.error(`btn ${id} not found`);
    return;
  }
  button.disabled = !enabled;
}

export function enforceDefault<T>(
  val: T | null | undefined,
  defaultValue: T
): T {
  return val == null ? defaultValue : val;
}

export function showModal({
  src = DEFAULT_IMG_SRC,
  height = '100%',
  width = '100%',
  alt = 'wikipedia image',
  title = 'title',
  wikipediaHref = '#',
}: {
  src?: string | null;
  height?: string | number | null;
  width?: string | number | null;
  alt?: string | null;
  title?: string | null;
  wikipediaHref?: string | null;
}): void {
  src = enforceDefault(src, DEFAULT_IMG_SRC);
  height = enforceDefault(height, '100%');
  width = enforceDefault(width, '100%');
  alt = enforceDefault(alt, 'wikipedia image');
  title = enforceDefault(title, 'title');
  wikipediaHref = enforceDefault(wikipediaHref, '#');
  const modal = document.getElementById('modal') as HTMLElement | null;
  if (!modal) {
    console.error(`modal not found`);
    return;
  }
  const img = modal.querySelector('#modal-img') as HTMLImageElement | null;
  if (!img) {
    console.error(`modal img not found`);
    return;
  }
  img.crossOrigin = 'anonymous';
  img.src = src;
  img.height = typeof height === 'number' ? height : parseInt(height, 10);
  img.width = typeof width === 'number' ? width : parseInt(width, 10);
  img.alt = alt;
  const mTitle = modal.querySelector('#modal-title') as HTMLElement | null;
  if (!mTitle) {
    console.error(`modal title not found`);
    return;
  }
  mTitle.textContent = title;
  const mA1 = modal.querySelector(
    '#modal-wikipedia-link'
  ) as HTMLAnchorElement | null;
  if (!mA1) {
    console.error(`modal wikipedia link not found`);
    return;
  }
  mA1.classList.remove('disabled-a');
  mA1.href = wikipediaHref;
  if (wikipediaHref === '#') {
    mA1.classList.add('disabled-a');
  }
  const bModal = new Modal(modal);
  bModal.show();
}

export function getNetworkOptions({
  isHierarchical = false,
  solver = Solvers.BARNES_HUT,
  direction = Directions.LR,
  isPhysicsEnabled = true,
  solverTestCoefficient = 1,
  smooth = {
    type: 'dynamic',
    roundness: 0.5,
  },
}: {
  isHierarchical: boolean;
  solver?: string | null;
  direction: string;
  isPhysicsEnabled?: boolean | null;
  solverTestCoefficient?: number;
  smooth?: {
    type: string;
    roundness: number;
  };
}): Options {
  return {
    nodes: {
      shape: 'dot',
      size: 10,
    },
    edges: {
      arrows: {
        to: {
          scaleFactor: 0.2,
        },
      },
      color: {
        color: '#848484',
        opacity: 0.8,
      },
      smooth: {
        enabled: true,
        type: smooth.type,
        roundness: smooth.roundness,
      },
      font: {
        color: '#343434',
        size: 12,
        align: 'middle',
      },
      selfReference: {
        size: 20,
        angle: Math.PI / 4,
        renderBehindTheNode: true,
      },
    },
    physics: {
      enabled: isPhysicsEnabled,
      solver: Object.values(Solvers).includes(solver as Solvers)
        ? solver
        : Solvers.BARNES_HUT,
      stabilization: { iterations: 3000 },
      barnesHut: {
        gravitationalConstant: -2000 * solverTestCoefficient,
        centralGravity: 0.3 / solverTestCoefficient,
        springLength: 95 * solverTestCoefficient,
        springConstant: 0.04 / solverTestCoefficient,
        avoidOverlap: 0,
      },
      forceAtlas2Based: {
        gravitationalConstant: -50 * solverTestCoefficient,
        centralGravity: 0.01 / solverTestCoefficient,
        springLength: 100 * solverTestCoefficient,
        springConstant: 0.08 / solverTestCoefficient,
        avoidOverlap: 1,
      },
      repulsion: {
        nodeDistance: 100 * solverTestCoefficient,
        centralGravity: 0.2 / solverTestCoefficient,
        springLength: 200 * solverTestCoefficient,
        springConstant: 0.05 / solverTestCoefficient,
      },
      hierarchicalRepulsion: {
        nodeDistance: 120 * solverTestCoefficient,
        centralGravity: 0.0,
        springLength: 100 * solverTestCoefficient,
        springConstant: 0.01 / solverTestCoefficient,
        avoidOverlap: 1,
      },
    },
    interaction: {
      hideEdgesOnDrag: true,
      hideEdgesOnZoom: true,
      hideNodesOnDrag: false,
      tooltipDelay: 300,
    },
    layout: {
      randomSeed: 1,
      improvedLayout: isPhysicsEnabled,
      clusterThreshold: 150,
      hierarchical: isHierarchical
        ? {
            levelSeparation: 150 * solverTestCoefficient,
            nodeSpacing: 100 * solverTestCoefficient,
            blockShifting: true,
            edgeMinimization: true,
            parentCentralization: false,
            direction: Object.values(Directions).includes(
              direction as Directions
            )
              ? direction
              : Directions.LR,
            sortMethod: 'directed',
            shakeTowards: 'roots',
          }
        : false,
    },
  };
}

export function downloadJSON(obj: object, filename: string): void {
  const json = JSON.stringify(obj, null, 2);
  const blob = new Blob([json], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function initTooltips(): Tooltip[] {
  const elements: NodeListOf<Element> = document.querySelectorAll(
    '[data-bs-toggle="tooltip"]'
  );
  Array.from(elements).forEach((el) => {
    const instance = Tooltip.getInstance(el);
    if (instance) instance.dispose();
  });
  return Array.from(elements).map((el) => new Tooltip(el));
}

export function initPopovers(): Popover[] {
  const elements: NodeListOf<Element> = document.querySelectorAll(
    '[data-bs-toggle="popover"]'
  );
  Array.from(elements).forEach((el) => {
    const instance = Popover.getInstance(el);
    if (instance) instance.dispose();
  });
  return Array.from(elements).map((el) => new Popover(el));
}

export function setupNumericInput(
  elementId: string,
  replacementChar: string
): void {
  const inputElement = document.getElementById(
    elementId
  ) as HTMLInputElement | null;
  if (!inputElement) {
    console.error(`${elementId} not found`);
    return;
  }
  inputElement.addEventListener('input', (event) => {
    const target = event.target as HTMLInputElement | null;
    if (!target) {
      console.error(`${elementId} target not found`);
      return;
    }
    target.value = target.value.replace(/[^0-9.]/g, replacementChar);
  });
}

export function safeDecodeURIComponent(str: string) {
  const sanitized = str.replace(/%(?![0-9A-Fa-f]{2})/g, '%25');
  try {
    return decodeURIComponent(sanitized);
  } catch {
    console.error(`${str} can't be decoded`);
    return str;
  }
}

export function setupGraphButton(
  handler: () => Promise<void>
): HTMLButtonElement | null {
  const graphBtn = document.getElementById(
    'graph-button'
  ) as HTMLButtonElement | null;
  if (!graphBtn) {
    console.error('graphBtn not found');
    return null;
  }
  graphBtn.addEventListener('click', async () => {
    clearGraph(wrapper);
    clearToasts();
    await handler();
  });
  return graphBtn;
}

export function setupDownloadButton(handler: () => Promise<void>): void {
  const dwnBtn = document.getElementById(
    'download-button'
  ) as HTMLButtonElement | null;
  if (!dwnBtn) {
    console.error('dwnBtn not found');
    return;
  }
  dwnBtn.addEventListener('click', async () => {
    clearToasts();
    await handler();
  });
}

export function setupClearButton(formFieldIds: string[]): void {
  const clearBtn = document.getElementById(
    'clear-button'
  ) as HTMLButtonElement | null;
  if (!clearBtn) {
    console.error(`clearBtn not found`);
    return;
  }
  clearBtn.addEventListener('click', () => {
    clearForm(formFieldIds);
    clearGraph(wrapper);
    clearToasts();
  });
}

export function setupSearchAutoComplete(id: string) {
  const input = document.getElementById(id) as HTMLInputElement | null;
  if (!input) {
    console.error(`input ${id} not found!`);
    throw new Error(`input ${id} not found!`);
  }
  const dropdown = document.getElementById(
    `${id}-dropdown`
  ) as HTMLInputElement | null;
  if (!dropdown) {
    console.error(`input ${id} not found!`);
    throw new Error(`input ${id} not found!`);
  }
  let activeIndex = -1;
  input.addEventListener(
    'input',
    debounce(async () => {
      const title = input.value.trim();
      if (!title) {
        return clearDropdown();
      }
      const data: SearchSuggestionsDto | null = await fetchData(
        `${BASE_URL}/api/wiki/title?title=${encodeURIComponent(title)}`
      );
      dropdown.innerHTML = '';
      activeIndex = -1;
      if (data?.pages?.length) {
        for (const page of data.pages) {
          const li = document.createElement('li');
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'dropdown-item d-flex align-items-start gap-2';
          btn.addEventListener('click', () => {
            input.value = page.key;
            clearDropdown();
          });
          const imgWrapper = document.createElement('div');
          imgWrapper.className = 'flex-shrink-0';
          const img = document.createElement('img');
          if (page.thumbnail?.source) {
            img.src = page.thumbnail.source.startsWith('http')
              ? page.thumbnail.source
              : `https:${page.thumbnail.source}`;
          } else {
            img.src = DEFAULT_IMG_SRC;
          }
          img.alt = page.title;
          img.className = 'rounded';
          img.style.width = '40px';
          img.style.height = '40px';
          img.style.objectFit = 'cover';
          imgWrapper.appendChild(img);
          btn.appendChild(imgWrapper);
          const textWrapper = document.createElement('div');
          textWrapper.className =
            'd-flex flex-column text-start overflow-hidden';
          const title = document.createElement('div');
          title.textContent = page.title;
          title.className = 'fw-semibold text-truncate';
          const desc = document.createElement('small');
          desc.textContent = page.description || '';
          desc.className = 'text-muted text-truncate';
          textWrapper.appendChild(title);
          textWrapper.appendChild(desc);
          btn.appendChild(textWrapper);
          li.appendChild(btn);
          dropdown.appendChild(li);
        }
        dropdown.classList.add('show');
      } else {
        clearDropdown();
      }
    }, 500)
  );

  document.addEventListener('click', (e) => {
    if (
      !input.contains(e.target as globalThis.Node) &&
      !dropdown.contains(e.target as globalThis.Node)
    ) {
      clearDropdown();
    }
  });

  input.addEventListener('keydown', (e) => {
    const items = dropdown.querySelectorAll<HTMLButtonElement>(
      'button.dropdown-item'
    );
    if (!items.length || !dropdown.classList.contains('show')) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      activeIndex = (activeIndex + 1) % items.length;
      updateActive(items);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      activeIndex = (activeIndex - 1 + items.length) % items.length;
      updateActive(items);
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault();
      items[activeIndex].click();
    } else if (e.key === 'Escape') {
      clearDropdown();
    }
  });

  function updateActive(items: NodeListOf<HTMLButtonElement>) {
    items.forEach((el, i) => el.classList.toggle('active', i === activeIndex));
    items[activeIndex]?.scrollIntoView({ block: 'nearest' });
  }

  function clearDropdown() {
    if (dropdown) {
      dropdown.classList.remove('show');
      dropdown.innerHTML = '';
      activeIndex = -1;
    }
  }

  function debounce<T extends (...args: any[]) => void>(fn: T, wait: number) {
    let t: number | undefined;
    return (...args: Parameters<T>) => {
      if (t !== undefined) window.clearTimeout(t);
      t = window.setTimeout(() => fn(...args), wait);
    };
  }
}
