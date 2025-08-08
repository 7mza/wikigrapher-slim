import '../scss/styles.scss';

import { Modal, Toast, Tooltip, Popover } from 'bootstrap';
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
  body?: any;
  code?: string;

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
    body?: any;
    code?: string;
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

export async function fetchThumbnail(
  url: string
): Promise<ThumbnailDto | null> {
  try {
    toggleSpinner({ show: true });
    setButtonState({ id: 'graph-button', enabled: false });
    setButtonState({ id: 'random-button', enabled: false });
    setButtonState({ id: 'clear-button', enabled: false });
    return await fetchData<ThumbnailDto>(url);
  } catch (error) {
    console.error(error);
    return {};
  } finally {
    toggleSpinner({ show: false });
    setButtonState({ id: 'graph-button' });
    setButtonState({ id: 'random-button' });
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
  focusNodeId?: string | number | null | undefined;
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
        data = await fetchThumbnail(
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
  message?: string;
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

export function showToast({
  message,
  theme = 'text-bg-danger',
}: {
  message: string;
  theme?: string;
}): void {
  document.querySelectorAll('.toast').forEach((toast) => toast.remove());
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
  closeButton.setAttribute('type', 'button');
  closeButton.className = 'btn-close me-2 m-auto';
  closeButton.setAttribute('data-bs-dismiss', 'toast');
  closeButton.setAttribute('aria-label', 'Close');
  flexDiv.appendChild(toastBody);
  flexDiv.appendChild(closeButton);
  toast.appendChild(flexDiv);
  toastContainer.appendChild(toast);
  const bsToast = new Toast(toast);
  bsToast.show();
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
  enabled?: boolean;
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
  src?: string;
  height?: string | number;
  width?: string | number;
  alt?: string;
  title?: string;
  wikipediaHref?: string;
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
  solver?: string;
  direction: string;
  isPhysicsEnabled?: boolean;
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

export class NodeDto {
  id: string;
  title: string;
  type: NodeDtoType;
  outgoing?: NodeDto[];
  incoming?: NodeDto[];
  categories?: CategoryDto[];

  constructor({
    id,
    title,
    type,
    outgoing,
    incoming,
    categories,
  }: {
    id: string;
    title: string;
    type: NodeDtoType;
    outgoing?: NodeDto[];
    incoming?: NodeDto[];
    categories?: CategoryDto[];
  }) {
    this.id = id;
    this.title = title;
    this.type = type;
    this.outgoing = outgoing;
    this.incoming = incoming;
    this.categories = categories;
  }
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
  source?: string;
  width?: number;
  height?: number;
}

export interface CategoryDto {
  id: string;
  title: string;
  contains?: NodeDto[];
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
  dwnBtn.addEventListener('click', async () => await handler());
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
  });
}
