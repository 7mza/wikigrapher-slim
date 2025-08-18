import {
  BASE_URL,
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
  initPopovers,
  initTooltips,
  MAX_NODE_SIZE,
  Node,
  NodeDto,
  NodeDtoType,
  RelationDto,
  renderNetwork,
  reverseInputs,
  safeDecodeURIComponent,
  setInputValue,
  setupClearButton,
  setupDownloadButton,
  setupGraphButton,
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
    setInputValue({ id: 'skip-input', value: 0 });
    setInputValue({ id: 'limit-input', value: 1 });
    const pages: NodeDto[] | null = await fetchWithUI(
      `${BASE_URL}/api/core/page/random?n=2`
    );
    if (pages && pages.length >= 2) {
      setInputValue({ id: 'source-input', value: pages![0].title });
      setInputValue({ id: 'target-input', value: pages![1].title });
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
    const { source, target, skip, limit } = getFormData();
    if (!source || !target) {
      showToast({
        message: 'Enter both source and target',
        theme: 'text-bg-warning',
      });
      return;
    }
    const data: RelationDto[] | null = await fetchWithUI(
      `${BASE_URL}/api/core/paths?sourceTitle=${encodeURIComponent(source)}&targetTitle=${encodeURIComponent(target)}&skip=${skip}&limit=${limit}`
    );
    if (data?.length) {
      const { parentNode, lastChildNode } = findParentAndLastChild(data);
      setInputValue({ id: 'source-input', value: parentNode.title });
      setInputValue({ id: 'target-input', value: lastChildNode.title });
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
              message: `Data too large, physics will be disabled`,
              theme: 'text-bg-warning',
            });
            return false;
          }
          return true;
        })(nodes.length),
      });
      await renderNetwork({
        wrapper: wrapper,
        nodes: nodes,
        edges: edges,
        options: options,
      });
    } else {
      showToast({
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
} {
  const source = getInputValue('source-input');
  const target = getInputValue('target-input');
  const skip = parseInt(getInputValue('skip-input')) || 0;
  const limit = parseInt(getInputValue('limit-input')) || 1;
  return { source, target, skip, limit };
}

(function listeners() {
  setupGraphButton(handleGraphButton);

  const randBtn = document.getElementById(
    'random-button'
  ) as HTMLButtonElement | null;
  if (!randBtn) {
    console.error('randBtn not found');
    return;
  }
  randBtn.addEventListener('click', async () => {
    clearGraph(wrapper);
    clearToasts();
    await handleRandomButton();
  });

  const reverseBtn = document.getElementById(
    'reverse-button'
  ) as HTMLButtonElement | null;
  if (!reverseBtn) {
    console.error('reverseBtn not found');
    return;
  }
  reverseBtn.addEventListener('click', () =>
    reverseInputs({
      sourceInputId: 'source-input',
      targetInputId: 'target-input',
    })
  );

  setupDownloadButton(handleDwnButton);

  setupClearButton([
    'source-input',
    'target-input',
    'skip-input',
    'limit-input',
  ]);

  setupNumericInput('skip-input', '0');
  setupNumericInput('limit-input', '1');

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

  setupSearchAutoComplete('source-input');
  setupSearchAutoComplete('target-input');

  initPopovers();
  initTooltips();
})();
