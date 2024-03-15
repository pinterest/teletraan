// Utils for base image selection UI components

function goldenImageFinder(image) {
    return image.golden;
}

function baseImageSorter(item1, item2) {
    return item2.publish_date - item1.publish_date;
}

function getDefaultBaseImageId(baseImagesSorted) {
    let baseImagesAcceptedSorted = baseImagesSorted.filter(e => e.acceptance == 'ACCEPTED');
    return baseImagesAcceptedSorted.length > 0 ? baseImagesAcceptedSorted[0].id : baseImagesSorted[0].id;
};

function mapBaseImagesToOptions(baseImagesSorted) {
    return baseImagesSorted.map(o => {
        const status = o.golden ? ' [CURRENT_GOLDEN]' : (o.acceptance && o.acceptance !== 'UNKNOWN' ? ` [${o.acceptance}]` : ''); 
        const date = new Date(o.publish_date);

        return {
            value: o.id,
            text: `${o.provider_name}${status} ${date.toLocaleDateString('en-US')}`,
            publishDate: o.publish_date,
        }
    });
}

function mapImageNameToOptions(imageNames) {
    return imageNames.sort().map(o => {
        return {
            value: o,
            text: o,
        };
    });
}

function ensureCurrentImageIsIncluded(baseImages, currentBaseImage) {
    if (!baseImages.some(image => image.id === currentBaseImage.id)) {
        baseImages.push(currentBaseImage);
    }
}

function isPinImageEnabled(goldenImage) {
    // If there is golden - enable Pin Image checkbox 
    // If there is no golden - disable Pin Image checkbox 
    return !!goldenImage;
}

function getPinImageValue(goldenImage, clusterAutoUpdateBaseImage) {
    // If there is golden: getPinImageValue = !clusterAutoUpdateBaseImage
    // if there is no golden: getPinImageValue = true. Always pin
    return !!goldenImage ? !clusterAutoUpdateBaseImage : true; 
}