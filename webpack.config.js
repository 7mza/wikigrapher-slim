'use strict';

import path, { dirname } from 'path';
import { fileURLToPath } from 'url';
import MiniCssExtractPlugin from 'mini-css-extract-plugin';
import CssMinimizerPlugin from 'css-minimizer-webpack-plugin';
import TerserPlugin from 'terser-webpack-plugin';
import autoprefixer from 'autoprefixer';
import { PurgeCSSPlugin } from 'purgecss-webpack-plugin';
import { glob } from 'glob';
import { WebpackAssetsManifest } from 'webpack-assets-manifest';
import webpack from 'webpack';
import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const MODE = process.env.NODE_ENV || 'production';

export default {
  mode: MODE,
  devtool: MODE === 'development' ? 'source-map' : false,
  entry: {
    vendor: {
      import: ['bootstrap', 'vis-network/peer', 'vis-data/peer', 'file-saver'],
    },
    shared: {
      import: './src/main/resources/static/ts/shared.ts',
      dependOn: 'vendor',
    },
    paths: {
      import: './src/main/resources/static/ts/paths.ts',
      dependOn: 'shared',
    },
  },
  output: {
    path: path.resolve(__dirname, './src/main/resources/static/dist/'),
    filename: '[name].[contenthash].min.js',
    clean: true,
  },
  module: {
    rules: [
      {
        test: /\.(js|ts)$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: [
                [
                  '@babel/preset-env',
                  { modules: false, useBuiltIns: 'entry', corejs: 3 },
                ],
              ],
            },
          },
          { loader: 'ts-loader', options: { transpileOnly: true } },
        ],
      },
      {
        test: /\.(scss)$/,
        use: [
          {
            loader: MiniCssExtractPlugin.loader,
          },
          {
            loader: 'css-loader',
          },
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: {
                plugins: [autoprefixer],
              },
            },
          },
          {
            loader: 'sass-loader',
          },
        ],
      },
      {
        test: /\.(woff|woff2|eot|ttf|svg)$/,
        type: 'asset/resource',
        generator: {
          filename: '[name][ext]',
        },
      },
    ],
  },
  resolve: {
    extensions: ['.js', '.ts', '.scss'],
  },
  plugins: [
    new webpack.DefinePlugin({ 'process.env.NODE_ENV': JSON.stringify(MODE) }),
    new ForkTsCheckerWebpackPlugin(),
    new MiniCssExtractPlugin({ filename: '[name].[contenthash].min.css' }),
    new PurgeCSSPlugin({
      paths: [
        ...glob.sync(
          path.join(__dirname, './src/main/resources/templates/**/*.html'),
          { nodir: true }
        ),
        ...glob.sync(
          path.join(__dirname, './src/main/resources/static/**/*.{js,ts}'),
          { nodir: true }
        ),
      ],
    }),
    new WebpackAssetsManifest({
      output: 'asset-manifest.json',
      publicPath: '/dist/',
      writeToDisk: true,
      customize(entry) {
        const cleanKey = entry.key.split('?')[0];
        return {
          key: cleanKey,
          value: entry.value,
        };
      },
    }),
  ],
  optimization: {
    splitChunks: {
      chunks: 'all',
    },
    minimize: true,
    minimizer: [
      new TerserPlugin({
        extractComments: false,
        terserOptions: {
          format: {
            comments: false,
          },
        },
      }),
      new CssMinimizerPlugin({
        minimizerOptions: {
          preset: [
            'default',
            {
              discardComments: { removeAll: true },
            },
          ],
        },
      }),
    ],
  },
  cache: {
    type: 'filesystem',
    buildDependencies: {
      config: [__filename],
    },
  },
};
