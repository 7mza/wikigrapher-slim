import {
  BASE_URL,
  clearForm,
  clearGraph,
  clearToasts,
  Colors,
  DataSet,
  downloadJSON,
  Edge,
  fetchWithUI,
  getCheckboxState,
  getInputValue,
  getNetworkOptions,
  getRequestParam,
  initPopovers,
  initTooltips,
  MAX_NODE_SIZE,
  Node,
  NodeDto,
  NodeDtoType,
  refitGraphOnResize,
  RelationDto,
  renderNetwork,
  reverseInputs,
  safeDecodeURIComponent,
  setInputValue,
  setupButton,
  setupNumericInput,
  setupSearchAutoComplete,
  showToast,
  toggleSelect,
  wrapper,
} from './shared';

function findParentAndLastChild(data: RelationDto[]): {
  parentNode: NodeDto;
  lastChildNode: NodeDto;
} {
  const parentRelation = data.find((rel) => rel.source.isTopParent);
  if (!parentRelation) {
    throw new Error('parentNode not found');
  }
  const lastChildRelation = data.find((rel) => rel.target.isBottomChild);
  if (!lastChildRelation) {
    throw new Error('lastChildNode not found');
  }
  return {
    parentNode: parentRelation.source,
    lastChildNode: lastChildRelation.target,
  };
}

async function prepareNetworkData({
  data,
  parentNode,
  lastChildNode,
}: {
  data: RelationDto[];
  parentNode: NodeDto;
  lastChildNode: NodeDto;
}): Promise<{ nodes: DataSet<Node>; edges: DataSet<Edge> }> {
  const nodes = new DataSet<Node>();
  const edges = new DataSet<Edge>();
  data.forEach(({ source, target }) => {
    addNode({
      node: source,
      parentNode: parentNode,
      lastChildNode: lastChildNode,
    });
    addNode({
      node: target,
      parentNode: parentNode,
      lastChildNode: lastChildNode,
    });
    edges.add({
      id: `${source.id}-${target.id}`,
      from: source.id,
      to: target.id,
      arrows: 'to',
      label: source.type === NodeDtoType.REDIRECT ? 'redirect_to' : 'link_to',
    });
  });
  return { nodes: nodes, edges: edges };

  function addNode({
    node,
    parentNode,
    lastChildNode,
  }: {
    node: NodeDto;
    parentNode: NodeDto;
    lastChildNode: NodeDto;
  }) {
    if (!nodes.get(node.id)) {
      nodes.add({
        id: node.id,
        label: safeDecodeURIComponent(node.title),
        title: `id: ${node.id}\ntitle: ${node.title}\ntype: ${node.type}`,
        color:
          node.id === parentNode.id
            ? Colors.ENTER
            : node.id === lastChildNode.id
              ? Colors.EXIT
              : node.type === NodeDtoType.REDIRECT
                ? Colors.REDIRECT
                : Colors.PAGE,
        type: node.type,
      });
    }
  }
}

async function handleRandomButton(): Promise<void> {
  try {
    const pages: NodeDto[] | null = await fetchWithUI(
      `${BASE_URL}/api/core/page/random?n=2`
    );
    if (pages && pages.length >= 2) {
      setInputValue({ id: 'sourceInput', value: pages![0].title });
      setInputValue({ id: 'targetInput', value: pages![1].title });
      await handleGraphButton();
    } else {
      await handleRandomButton(); // FIXME: danger
    }
  } catch (error) {
    console.error(error);
  }
}

async function handleGraphButton(): Promise<void> {
  try {
    const { source, target, skip, limit, solverTestCoefficient } =
      getFormData();
    if (!source || !target) {
      showToast({
        id: 'toast-container',
        message: 'Enter both source and target',
        theme: 'text-bg-warning',
      });
      return;
    }
    const encodedSourceTitle = encodeURIComponent(source);
    const encodedTargetTitle = encodeURIComponent(target);
    const relativePath = `?sourceTitle=${encodedSourceTitle}&targetTitle=${encodedTargetTitle}&skip=${skip}&limit=${limit}`;
    history.pushState(null, '', `${relativePath}&fetch=true`);
    const data: RelationDto[] | null = await fetchWithUI(
      `${BASE_URL}/api/core/paths${relativePath}`
    );
    if (data?.length) {
      const { parentNode, lastChildNode } = findParentAndLastChild(data);
      setInputValue({ id: 'sourceInput', value: parentNode.title });
      setInputValue({ id: 'targetInput', value: lastChildNode.title });
      const { nodes: nodes, edges: edges } = await prepareNetworkData({
        data: data,
        parentNode: parentNode,
        lastChildNode: lastChildNode,
      });
      const options = getNetworkOptions({
        isHierarchical: getCheckboxState('hierarchical-checkbox'),
        solver: getInputValue('solver-select'),
        direction: getInputValue('direction-select'),
        isPhysicsEnabled: ((size) => {
          if (size > MAX_NODE_SIZE) {
            showToast({
              id: 'toast-container',
              message: `Data too large, physics will be disabled`,
              theme: 'text-bg-warning',
            });
            return false;
          }
          return true;
        })(nodes.length),
        solverTestCoefficient: solverTestCoefficient,
      });
      await renderNetwork({
        id: 'graph',
        wrapper: wrapper,
        nodes: nodes,
        edges: edges,
        options: options,
      });
    } else {
      showToast({
        id: 'toast-container',
        message: `No path found between "${source}" and "${target}"`,
        theme: 'text-bg-warning',
      });
    }
  } catch (error) {
    console.error(error);
  }
}

async function handleDwnButton(): Promise<void> {
  try {
    const { source, target } = getFormData();
    if (!source || !target) {
      showToast({
        id: 'toast-container',
        message: 'Enter both source and target',
        theme: 'text-bg-warning',
      });
      return;
    }
    const data: RelationDto[] | null = await fetchWithUI(
      `${BASE_URL}/api/core/paths/all?sourceTitle=${encodeURIComponent(source)}&targetTitle=${encodeURIComponent(target)}`
    );
    if (data?.length) {
      downloadJSON(data, `${source}_${target}.json`);
    } else {
      showToast({
        id: 'toast-container',
        message: `No path found between "${source}" and "${target}"`,
        theme: 'text-bg-warning',
      });
    }
  } catch (error) {
    console.error(error);
  }
}

function getFormData(): {
  source: string;
  target: string;
  skip: number;
  limit: number;
  solverTestCoefficient: number;
} {
  const source = getInputValue('sourceInput');
  const target = getInputValue('targetInput');
  const skip = parseInt(getInputValue('skipInput')) || 0;
  const limit = parseInt(getInputValue('limitInput')) || 1;
  const solverTestCoefficient =
    parseFloat(getInputValue('solverTestCoefficientInput')) || 1;
  return { source, target, skip, limit, solverTestCoefficient };
}

(function listeners() {
  const graphBtn = setupButton('graphBtn', async () => {
    clearGraph(wrapper);
    clearToasts();
    await handleGraphButton();
  });

  if (getRequestParam('fetch') === 'true') {
    graphBtn!.click();
  }

  setupButton('randomBtn', async () => {
    clearGraph(wrapper);
    clearToasts();
    await handleRandomButton();
  });

  setupButton('reverseBtn', async () => {
    reverseInputs({
      sourceInputId: 'sourceInput',
      targetInputId: 'targetInput',
    });
  });

  setupButton('downloadBtn', async () => {
    clearToasts();
    await handleDwnButton();
  });

  setupButton('clearBtn', async () => {
    history.pushState(null, '', '/');
    clearForm([
      'sourceInput',
      'targetInput',
      'skipInput',
      'limitInput',
      'solverTestCoefficientInput',
    ]);
    clearGraph(wrapper);
    clearToasts();
  });

  setupNumericInput('skipInput', '0');
  setupNumericInput('limitInput', '5');

  const hierarchicalCheckbox = document.getElementById(
    'hierarchical-checkbox'
  ) as HTMLInputElement | null;
  if (!hierarchicalCheckbox) {
    console.error('hierarchical-checkbox not found');
    return;
  }
  hierarchicalCheckbox.addEventListener('change', (event) => {
    const target = event.target as HTMLInputElement | null;
    if (!target) {
      console.error('hierarchicalCheckbox target not found');
      return;
    }
    toggleSelect({ id: 'solver-select', enabled: !target.checked });
    toggleSelect({ id: 'direction-select', enabled: target.checked });
  });

  document.addEventListener('DOMContentLoaded', () => {
    const isHierarchical = getCheckboxState('hierarchical-checkbox');
    toggleSelect({ id: 'solver-select', enabled: !isHierarchical });
    toggleSelect({ id: 'direction-select', enabled: isHierarchical });
  });

  // setupButton('dwnGraphBtn', async () => {
  //   exportVisNetworkImage('graph');
  // });

  setupSearchAutoComplete('sourceInput');
  setupSearchAutoComplete('targetInput');

  initPopovers();
  initTooltips();

  refitGraphOnResize();
})();
