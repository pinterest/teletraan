module.exports = {
    env: {
        browser: true,
        es6: true,
        node: true,
        jest: true
    },
    globals: {},
    parser: 'babel-eslint',
    parserOptions: {
        ecmaVersion: 8,
        ecmaFeatures: {
            'jsx': true,
            experimentalObjectRestSpread: true
        },
        sourceType: 'module'
    },
    extends: [
        'prettier',
        // 'standard',
        'eslint:recommended',
        'standard-jsx',
        'standard-react'
    ],
    plugins: ['prettier', 'react'], // activating esling-plugin-prettier (--fix stuff)
    rules: {
        'no-unused-vars': 'off',
        'no-console': 'off',

        'jsx-quotes': 'off',
        'react/self-closing-comp': 'off',
        'react/jsx-indent': 'off',
        'react/jsx-indent-props': 'off',
        'react/prop-types': 'off',
        'jsx-a11y/href-no-hash': 'off',
        'react/jsx-tag-spacing': 'off',
        'react/jsx-no-bind': [
            'warn',
            {
                'allowArrowFunctions': true,
                'allowBind': false,
                'ignoreRefs': true
            }
        ],
        'react/no-did-update-set-state': 'warn',
        'react/no-unknown-property': 'warn',
        'react/no-unused-prop-types': 'warn',
        'react/react-in-jsx-scope': 'warn',

        'no-param-reassign': 'off',
        'no-underscore-dangle': 'off',
        'prefer-const': 'off',
        'semi': ['off'], // let prettier do its job
        'comma-dangle': 'off', // let prettier do its job with es5
        'arrow-spacing': 'warn',
        'block-spacing': 'warn',
        'comma-spacing': 'warn',
        'comma-style': 'warn',
        'dot-notation': 'warn',
        'eqeqeq': 'warn',
        'curly': ['warn', 'all'],
        'brace-style': ['warn', '1tbs', {'allowSingleLine': true}],
        'eol-last': 'warn',
        'key-spacing': 'warn',
        'keyword-spacing': 'warn',
        'linebreak-style': ['warn', 'unix'],
        'no-tabs': 'warn',
        'no-trailing-spaces': 'warn',
        'no-var': 'warn',
        'no-whitespace-before-property': 'warn',
        'semi-spacing': 'warn',
        'space-before-blocks': 'warn',
        'space-before-function-paren': ['off'],
        'space-in-parens': 'warn',
        'prettier/prettier': [ // customizing prettier rules (unfortunately not many of them are customizable)
            'off',
            {
                'useTabs': false,
                'printWidth': 120,
                'tabWidth': 2,
                'singleQuote': true,
                'trailingComma': 'none',
                'jsxBracketSameLine': true,
                'parser': 'babylon',
                'semi': false,
                'bracketSpacing': false,
                'arrowParens': 'avoid'
            }
        ]
    }
}
