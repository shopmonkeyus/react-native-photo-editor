import { NativeEventEmitter, NativeModules } from 'react-native';

export type Options = {
  path: String;
  stickers: Array<String>;
  translations: Array<String> | undefined;
};

export type ErrorCode =
  | 'USER_CANCELLED'
  | 'IMAGE_LOAD_FAILED'
  | 'ACTIVITY_DOES_NOT_EXIST'
  | 'FAILED_TO_SAVE_IMAGE'
  | 'DONT_FIND_IMAGE'
  | 'ERROR_UNKNOW';

type PhotoEditorCallback = (data: string) => void;

type PhotoEditorType = {
  open(option: Options): Promise<String>;
  addListener(event: string, callback: PhotoEditorCallback): void;
  removeListeners(event: string): void;
};

const { PhotoEditor } = NativeModules;

let exportObject: PhotoEditorType = {} as PhotoEditorType;
const eventEmitter = new NativeEventEmitter(PhotoEditor);

const defaultOptions = {
  path: '',
  stickers: [],
  translations: ["cancel", "done", "draw", "stickers", "text"],
};

let subscription: any;

exportObject = {
  open: (optionsEditor: Options) => {
    const options = {
      ...defaultOptions,
      ...optionsEditor,
    };
    return new Promise(async (resolve, reject) => {
      try {
        const response = await PhotoEditor.open(options);
        if (response) {
          resolve(response);
          return;
        }
        throw 'ERROR_UNKNOW';
      } catch (e) {
        reject(e);
      }
    });
  },
  addListener: (event: string, callback: PhotoEditorCallback) => {
    subscription = eventEmitter.addListener(event, data => {
      callback(data);
    })
  },
  removeListeners: (event: string) => {
    if (subscription) {
      subscription.remove()
    }

    eventEmitter.removeAllListeners(event)
  },
};

export default exportObject;
