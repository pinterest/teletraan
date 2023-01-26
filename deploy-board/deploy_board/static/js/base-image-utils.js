// utils for base image selection components

function goldenImageFinder(image) {
    if (image.golden) {
        return image;
    }
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
        let suffix = (o.acceptance ? `[${o.acceptance}]` : '') + `${o.golden ? '[GOLDEN]' : ''}`;
        return {
            value: o.id,
            text: o.provider_name + suffix,
        }
    });
}

function mapImageNameToOptions(imageNames) {
    return imageNames.map(o => {
        return {
            value: o,
            text: o,
        };
    });
}
