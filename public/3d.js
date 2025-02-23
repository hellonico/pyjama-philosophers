function addModelViewer(modelUrl) {
    const container = document.getElementsByClassName("modelContainer")[0];
    addModelViewerToContainer(container, modelUrl)
}

function addModelViewerToContainer(container, modelUrl) {

    // Remove existing model-viewer if any
    container.innerHTML = "";

    // Create new model-viewer element
    const modelViewer = document.createElement("model-viewer");
    modelViewer.setAttribute("camera-controls", "");
    modelViewer.setAttribute("touch-action", "pan-y");
    modelViewer.setAttribute("autoplay", "");
    modelViewer.setAttribute("ar", "");
    modelViewer.setAttribute("ar-modes", "webxr scene-viewer");
    modelViewer.setAttribute("scale", "0.2 0.2 0.2");
    modelViewer.setAttribute("shadow-intensity", "1");
    modelViewer.setAttribute("src", modelUrl);
    modelViewer.setAttribute("alt", "An animated 3D model");

    container.appendChild(modelViewer);
}