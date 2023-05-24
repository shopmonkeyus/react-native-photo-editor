import { NativeEventEmitter, NativeModules } from 'react-native';
const { PhotoEditor } = NativeModules;

let exportObject = {};
const eventEmitter = new NativeEventEmitter(PhotoEditor);

const defaultOptions = {
  path: '',
  stickers: [],
};

let subscription;

exportObject = {
  open: (optionsEditor) => {
    const options = {
      ...defaultOptions,
      ...optionsEditor,
    };
    return new Promise(async (resolve, reject) => {
      try {
        const response = await PhotoEditor.open(options);
        if (response) {
          resolve(response);
          return true;
        }
        throw 'ERROR_UNKNOW';
      } catch (e) {
        reject(e);
      }
    });
  },
  addListener: (event, callback) => {
    subscription = eventEmitter.addListener(event, data => {
      callback(data);
    })
  },
  removeListeners: (event) => {    
    if (subscription) {
      subscription.remove()
    }
    
    eventEmitter.removeAllListeners(event)
  },
};

export default exportObject;
